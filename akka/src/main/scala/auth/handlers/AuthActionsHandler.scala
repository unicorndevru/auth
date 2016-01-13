package auth.handlers

import akka.http.javadsl.model.ResponseEntity
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, StatusCodes }
import akka.stream.Materializer
import auth.api.{ChangeEmailCommand, EmailVerifyCommand, PasswordRecoverCommand}
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

  val emptyAnswer = StatusCodes.NoContent -> HttpEntity.empty(ContentTypes.NoContentType)

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
          onSuccess(passwordChangeService.changePassword(status.userId, pc.oldPass.getOrElse(""), pc.newPass)) { _ ⇒
            complete(status)
          }
        } ~ (path("startPasswordRecovery") & entity(as[StartPasswordRecover])) { spr =>
          onSuccess(passwordRecoveryService.startRecovery(spr.email)) {
            complete(emptyAnswer)
          }
        } ~ (path("checkPasswordRecovery") & authTokenCommand[PasswordRecoverCommand, CheckPasswordRecoverToken]) { _ =>
          complete(emptyAnswer)
        } ~ (path("recoverPassword") & authTokenExpirableCommand[PasswordRecoverCommand, FinishPasswordRecover]() & userAware) { (cmd, status) =>
          onSuccess(passwordRecoveryService.finishRecovery(cmd._1.email, cmd._2.newPass)) { pid =>
            val auth = status.getOrElse(AuthStatus(pid, Seq.empty, None))
            respondWithAuth(auth) {
              complete(auth)
            }
          }
        } ~ (path("verifyEmail") & authTokenCommand[EmailVerifyCommand, EmailVerifyToken]) { cmd =>
          onSuccess(emailVerifierService.verify(cmd._1.email)) {
            complete(emptyAnswer)
          }
        } ~ (path("requestEmailVerify") & userRequired) { s ⇒
          complete(
            emailVerifierService.startVerify(s.userId)
              .map(_ ⇒ emptyAnswer)
          )
        } ~ (path("checkEmailAvailability") & entity(as[EmailCheckAvailability])) { eca =>
          onSuccess(service.isEmailRegistered(eca.email)) {
            case true => complete(emptyAnswer)
            case false => failWith(AuthError.UserAlreadyRegistered)
          }
        } ~ (path("startEmailChange") & userRequired & entity(as[StartEmailChange])) { (status, cmd) =>
          emailChangeService.start(status.userId, cmd.email)
          complete(emptyAnswer)
        } ~ (path("finishEmailChange") & authTokenExpirableCommand[ChangeEmailCommand, FinishEmailChange]() & userAware) { (cmd, status) =>
          onSuccess(emailChangeService.finish(cmd._1.userId, cmd._1.newEmail)) { pid =>
            val auth = status.getOrElse(AuthStatus(pid, Seq.empty, None))
            respondWithAuth(auth) {
              complete(auth)
            }
          }
        }
      }
    }
}
