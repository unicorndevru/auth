package auth.handlers

import akka.http.javadsl.model.ResponseEntity
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, StatusCodes }
import akka.stream.Materializer
import auth.directives._
import auth.protocol._
import auth.providers.email.EmailPasswordServices
import auth.services.AuthService
import de.heikoseeberger.akkahttpcirce.CirceSupport
import akka.http.scaladsl.server.Directives._

import scala.concurrent.ExecutionContext

class AuthActionsHandler(service: AuthService, emailPasswordServices: EmailPasswordServices, override val authParams: AuthParams)(implicit ec: ExecutionContext, mat: Materializer)
    extends CirceSupport with AuthPermissionsDirectives with AuthCirceEncoders with AuthCirceDecoders {

  override val authService = service

  import emailPasswordServices._

  val switchRoute = (path("switch") & userRequired) { status ⇒
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
              respondWithAuth(st)(complete(st))
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
  }

  val route =
    pathPrefix("actions") {
      switchRoute ~ post {
        (path("changePassword") & userRequired & entity(as[PasswordChange])) { (status, pc) ⇒
          onSuccess(passwordChangeService.changePassword(status.userId, pc.oldPass.getOrElse(""), pc.newPass)){ _ ⇒
            complete(status)
          }
        } ~ (path("startPasswordRecovery") & userRequired & entity(as[StartPasswordRecover])) { (status, spr) =>
          onSuccess(passwordRecoveryService.startRecovery(spr.email)){
            complete(status)
          }
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
        } ~ (path ("requestEmailVerify") & userRequired) { s ⇒
          complete(
            emailVerifierService.startVerify(s.userId)
              .map(_ ⇒ StatusCodes.NoContent → HttpEntity.empty(ContentTypes.NoContentType))
          )
        } ~ (path ("checkEmailAvailability") & entity(as[EmailCheckAvailability])) { eca =>
          onSuccess(profileRegistrarService.isEmailRegistered(eca.email)) { result =>
            if (!result) {
              complete(StatusCodes.NoContent)
            } else {
              failWith(AuthError.UserAlreadyRegistered)
            }

          }
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
