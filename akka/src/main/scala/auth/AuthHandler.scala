package auth

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import auth.api.{ AuthCirceDecoders, AuthCirceEncoders, AuthDirectives }
import auth.core.UserIdentityService
import auth.data.identity.UserIdentity
import auth.protocol._
import auth.services.AuthService
import de.heikoseeberger.akkahttpcirce.CirceSupport

import scala.concurrent.{ ExecutionContext, Future }

class AuthHandler(service: AuthService)(implicit ec: ExecutionContext, mat: Materializer) extends CirceSupport with AuthDirectives with AuthCirceEncoders with AuthCirceDecoders {

  def authorize(cmd: AuthorizeCommand) =
    onSuccess(service.authorize(cmd)) {
      case Some(s) ⇒
        respondWithAuth(s) {
          complete(s)
        }
      case None ⇒
        failWith(AuthError.InvalidCredentials)
    }

  def register(cmd: AuthorizeCommand) =
    onSuccess(service.register(cmd)) { s ⇒
      respondWithAuth(s) {
        complete(StatusCodes.Created → s)
      }
    }

  val route =
    pathPrefix("auth") {
      pathEndOrSingleSlash {
        (get & userRequired) { status ⇒
          complete(status)

        } ~ post {
          entity(as[AuthByToken])(authorize) ~ entity(as[AuthByCredentials])(authorize)

        } ~ delete {
          failWith(AuthError.Unauthorized)

        } ~ put {
          entity(as[AuthByToken])(register) ~ entity(as[AuthByCredentials])(register)
        }
      }
    }

}

class IdentitiesHandler(service: UserIdentityService)(implicit ec: ExecutionContext, mat: Materializer) extends CirceSupport with AuthDirectives with AuthCirceEncoders with AuthCirceDecoders {

  def identityToProtocol(i: UserIdentity): AuthIdentity =
    AuthIdentity(
      id = i._id.get,
      identityId = AuthIdentityId(id = i.identityId.userId, provider = i.identityId.providerId),
      email = i.email,
      isEmailVerified = i.isEmailVerified
    )

  val route =
    (pathPrefix("identities") & userRequired) { status ⇒
      (get & pathEndOrSingleSlash) {
        val f = IdentitiesFilter(profileId = Some(status.userId))
        complete(service.query(f).map(is ⇒ AuthIdentitiesList(f, is.map(identityToProtocol))))
      } ~ (get & path(Segment)) { id ⇒
        complete(service.get(id).flatMap { ui ⇒
          if (ui.profileId.contains(status.userId)) {
            Future.successful(identityToProtocol(ui))
          } else {
            Future.failed(AuthError.IdentityNotFound)
          }
        })
      }
    }

}

class AuthActionsHandler()(implicit ec: ExecutionContext, mat: Materializer) extends CirceSupport with AuthDirectives with AuthCirceEncoders with AuthCirceDecoders {

  val route =
    pathPrefix("actions") {
      path("switch") {
        post {
          /*
          AuthorizedAction(Permission.SwitchUser).apply(parse.json[SwitchUserCommand]) {
    implicit request ⇒
      jwtAuthenticator.become(request.body.userId, Ok)
  }
           */
          complete("???")
        } ~ delete {
          // jwtAuthenticator.unbecome
          complete("???")
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