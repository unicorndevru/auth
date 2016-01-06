package auth.handlers

import akka.stream.Materializer
import auth.api._
import auth.protocol.SwitchUserCommand
import auth.services.AuthService
import de.heikoseeberger.akkahttpcirce.CirceSupport
import akka.http.scaladsl.server.Directives._

import scala.concurrent.ExecutionContext

class AuthActionsHandler(service: AuthService, override val authParams: AuthParams)(implicit ec: ExecutionContext, mat: Materializer) extends CirceSupport with AuthPermissionsDirectives with AuthCirceEncoders with AuthCirceDecoders {

  override val authService = service

  val route =
    pathPrefix("actions") {
      (path("switch") & userRequired) { status ⇒
        (post & entity(as[SwitchUserCommand])) { cmd ⇒
          cmd.userId match {
            case id if id == status.userId ⇒
              respondWithAuth(status)(complete(status))

            case id if status.originUserId.contains(id) ⇒
              // always allow
              onSuccess(service.getStatus(id)){ s ⇒
                respondWithAuth(s)(complete(s))
              }

            case id ⇒
              permissionsRequired(status, "switch") { _ ⇒
                onSuccess(service.getStatus(id)){ s ⇒
                  val st = s.copy(originUserId = status.originUserId orElse Some(status.userId))
                  respondWithAuth(s)(complete(s))
                }
              }
          }
        } ~ delete {
          status.originUserId match {
            case Some(o) ⇒
              onSuccess(service.getStatus(o)){ s ⇒
                respondWithAuth(s)(complete(s))
              }

            case None ⇒
              complete(status)
          }
        }
      } ~ post {
        path ("changePassword") {
          /*
          UserRequiredAction.async(authBodyJson[PasswordChange]) {
    implicit request ⇒
      for {
        _ ← passwordChangeService.changePassword(request.userId, request.body.oldPass.getOrElse(""), request.body.newPass)
        aStatus ← authStatus(request.userId, false)
      } yield aStatus.ok
  }
           */
          complete("???")
        } ~ path ("startPasswordRecovery") {
          /*
          Action.async(authBodyJson[StartPasswordRecover]) {
    implicit request ⇒
      for {
        _ ← passwordRecoveryService.startRecovery(request.body.email)
      } yield NoContent
  }
           */
          complete("???")
        } ~ path ("checkPasswordRecovery") {
          /*
           Action.apply(authToken[CheckPasswordRecoverToken, PasswordRecoverCommand]) {
    implicit request ⇒
      NoContent
  }
           */
          complete("???")
        } ~ path ("recoverPassword") {
          /*
          Action.async(authTokenExpirableJson[FinishPasswordRecover, PasswordRecoverCommand]()) {
    implicit request ⇒
      (for {
        pid ← passwordRecoveryService.finishRecovery(request.body.token.email, request.body.json.newPass)
        aStatus ← authStatus(pid, false)
      } yield aStatus.ok) recoverWith {
        case e: JsonApiFailure ⇒ e.copy(status = 401).resultF
      }
  }
           */
          complete("???")
        } ~ path ("verifyEmail") {
          /*
          Action.async(authToken[EmailVerifyToken, EmailVerifyCommand]) {
    implicit request ⇒
      (for {
        result ← emailVerifierService.verify(request.body.email)
      } yield NoContent) recover {
        case e: JsonApiFailure ⇒ e.result
        case u: Exception      ⇒ JsonApiFailure(400, "cannot_verify_email", u.getMessage, "auth").result
      }
  }
           */
          complete("???")
        } ~ path ("requestEmailVerify") {
          /*
          UserRequiredAction.apply {
    implicit request ⇒
      emailVerifierService.startVerify(request.userId)
      NoContent
  }
           */
          complete("???")
        } ~ path ("checkEmailAvailability") {
          /*
          Action.async(authBodyJson[EmailCheckAvailability]) {
    implicit request ⇒
      profileRegistrar
        .isEmailRegistered(request.body.email).map {
          case true ⇒
            JsonApiFailure(400, "email_already_registered", "Email already registered", "auth").result
          case false ⇒
            NoContent
        }
  }
           */
          complete("???")
        } ~ path ("startEmailChange") {
          /*
          UserRequiredAction.apply(authBodyJson[StartEmailChange]) {
    implicit request ⇒
      emailChangeService.start(request.userId, request.body.email)
      NoContent
  }
           */
          complete("???")
        } ~ path ("finishEmailChange") {
          /*
          Action.async(authTokenExpirable[FinishEmailChange, ChangeEmailCommand]()) {
    implicit request ⇒
      for {
        userId ← emailChangeService.finish(request.body.userId, request.body.newEmail)
        aStatus ← authStatus(userId)
      } yield aStatus.ok
  }

           */
          complete("???")
        }
      }
    }
}
