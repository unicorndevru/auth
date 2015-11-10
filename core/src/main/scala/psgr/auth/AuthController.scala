package psgr.auth

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc._
import psgr.auth.actions._
import psgr.auth.core.services._
import psgr.auth.facade._
import psgr.auth.protocol._
import psgr.expander.protocol.MetaRef
import psgr.failures.JsonApiFailure

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthController @Inject() (
    authUsersService:        AuthUsersService,
    userIdentityService:     UserIdentityService,
    userFinderService:       UserFinderService,
    cryptoService:           CommandCryptoService,
    jwtAuthenticator:        JwtAuthenticator,
    profileAuthenticator:    ProfileAuthenticatorService,
    profileRegistrar:        ProfileRegistrarService,
    passwordRecoveryService: PasswordRecoveryService,
    emailVerifierService:    EmailVerifierService,
    emailChangeService:      EmailChangeService,
    passwordChangeService:   PasswordChangeService,
    authBodyParsers:         AuthBodyParsers
) extends Controller with AuthControllerHelper {

  implicit class FailureResult(f: JsonApiFailure) {
    def result = Results.Status(f.status)(Json.toJson(f))

    def resultF = Future successful result
  }

  implicit class StatusOk(s: AuthStatus) {
    def ok = Ok(Json.toJson(s))
  }

  implicit class AuthUserIdOk(id: AuthUserId) {
    def ok = Ok(Json.toJson(Map("id" → id.id)))
  }

  import authBodyParsers._

  def authStatus(pid: AuthUserId, isSwitched: Boolean): Future[AuthStatus] = {
    for {
      perms ← authEnv.permissionsService.permissions(pid)
    } yield AuthStatus(
      user = authUsersService.toRef(pid),
      roles = perms.map(_.name).toSeq,
      isSwitched = Some(isSwitched).filter(identity),
      identities = AuthIdentitiesList.metaId
    )
  }

  def authStatus(pid: AuthUserId)(implicit rh: RequestHeader): Future[AuthStatus] =
    authStatus(pid, jwtAuthenticator.isSwitched)

  def become = AuthorizedAction(Permission.SwitchUser).apply(parse.json) {
    implicit request ⇒
      (request.body \ "user").validate[MetaRef[Any]].asOpt match {
        case Some(metaHref) ⇒
          jwtAuthenticator.become(AuthUserId(metaHref.meta.href.split("/").last), Ok)
        case None ⇒
          JsonApiFailure(BAD_REQUEST, "illegal_argument", "Incorrect json", "global").result
      }

  }

  @deprecated("Use new action switch", "14.10.2015")
  def oldBecome(profileId: String) = AuthorizedAction(Permission.SwitchUser).async {
    implicit request ⇒
      val aid = AuthUserId(profileId)
      for {
        r ← authStatusResp(aid)
      } yield jwtAuthenticator.become(aid, r)
  }

  def unbecome = UserRequiredAction {
    implicit request ⇒
      jwtAuthenticator.unbecome
  }

  def authStatusResp(pid: AuthUserId)(implicit rh: RequestHeader) =
    for {
      s ← authStatus(pid)
    } yield s.ok

  def status = UserRequiredAction.async {
    implicit request ⇒
      authStatusResp(request.userId)
  }

  def stop = Action {
    request ⇒
      jwtAuthenticator.clean(authEnv.unauthorizedResponse(request))
  }

  def start = Action.async(authBodyJson[AuthorizeCommand]) {
    implicit request ⇒
      profileAuthenticator.authorize(request.body).flatMap {
        case Some(pid) ⇒
          authStatus(pid, false)
            .map(_.ok)
            .map(jwtAuthenticator.authorize(pid))
        case None ⇒
          JsonApiFailure(400, "wrong_credentials", "Credentials you provide are not valid", "auth", Some(Json.toJson(request.body))).resultF
      }
  }

  def register = Action.async(authBodyJson[AuthorizeCommand]) {
    implicit request ⇒
      for {
        pid ← profileRegistrar.register(request.body)
        aStatus ← authStatus(pid, false)
      } yield jwtAuthenticator.authorize(pid)(Created(Json.toJson(aStatus)))
  }

  def changePassword = UserRequiredAction.async(authBodyJson[PasswordChange]) {
    implicit request ⇒
      for {
        _ ← passwordChangeService.changePassword(request.userId, request.body.oldPass.getOrElse(""), request.body.newPass)
        aStatus ← authStatus(request.userId, false)
      } yield aStatus.ok
  }

  def startRecover = Action.async(authBodyJson[StartPasswordRecover]) {
    implicit request ⇒
      for {
        _ ← passwordRecoveryService.startRecovery(request.body.email)
      } yield NoContent
  }

  def checkRecoverToken = Action.apply(authToken[CheckPasswordRecoverToken, PasswordRecoverCommand]) {
    implicit request ⇒
      NoContent
  }

  def recoverPassword = Action.async(authTokenExpirableJson[FinishPasswordRecover, PasswordRecoverCommand]()) {
    implicit request ⇒
      (for {
        pid ← passwordRecoveryService.finishRecovery(request.body.token.email, request.body.json.newPass)
        aStatus ← authStatus(pid, false)
      } yield aStatus.ok) recoverWith {
        case e: JsonApiFailure ⇒ e.copy(status = 401).resultF
      }
  }

  def checkEmailAvailability = Action.async(authBodyJson[EmailCheckAvailability]) {
    implicit request ⇒
      profileRegistrar
        .isEmailRegistered(request.body.email).map {
          case true ⇒
            JsonApiFailure(400, "email_already_registered", "Email already registered", "auth").result
          case false ⇒
            NoContent
        }
  }

  def startEmailVerifying = UserRequiredAction.apply {
    implicit request ⇒
      emailVerifierService.startVerify(request.userId)
      NoContent
  }

  def verifyEmail = Action.async(authToken[EmailVerifyToken, EmailVerifyCommand]) {
    implicit request ⇒
      (for {
        result ← emailVerifierService.verify(request.body.email)
      } yield NoContent) recover {
        case e: JsonApiFailure ⇒ e.result
        case u: Exception      ⇒ JsonApiFailure(400, "cannot_verify_email", u.getMessage, "auth").result
      }
  }

  def startChangeEmail = UserRequiredAction.apply(authBodyJson[StartEmailChange]) {
    implicit request ⇒
      emailChangeService.start(request.userId, request.body.email)
      NoContent
  }

  def finishChangeEmail = Action.async(authTokenExpirable[FinishEmailChange, ChangeEmailCommand]()) {
    implicit request ⇒
      for {
        userId ← emailChangeService.finish(request.body.userId, request.body.newEmail)
        aStatus ← authStatus(userId)
      } yield aStatus.ok
  }

  def getIdByEmail(email: String) = Action.async {
    userFinderService
      .getIdByEmail(email)
      .map(_.ok)
  }
}