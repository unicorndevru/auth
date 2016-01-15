package auth.handlers

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.Materializer
import auth.api._
import auth.directives._
import auth.protocol._
import auth.providers.email.EmailPasswordServices
import auth.services.AuthService
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.Decoder

import scala.concurrent.ExecutionContext
import scala.util.Success

class AuthActionsHandler(crypt: CredentialsCommandCrypto, service: AuthService, emailPasswordServices: EmailPasswordServices, override val authParams: AuthParams)(implicit ec: ExecutionContext, mat: Materializer)
    extends CirceSupport with AuthPermissionsDirectives with AuthCirceEncoders with AuthCirceDecoders {

  override val authService = service

  val emptyResponse = StatusCodes.NoContent → HttpEntity.empty(ContentTypes.NoContentType)

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
        } ~ (path("startPasswordRecovery") & entity(as[StartPasswordRecover])) { spr ⇒
          onSuccess(passwordRecoveryService.startRecovery(spr.email)) {
            complete(emptyResponse)
          }
        } ~ (path("checkPasswordRecovery") & authTokenCommand[PasswordRecoverCommand, CheckPasswordRecoverToken]) { _ ⇒
          complete(emptyResponse)
        } ~ (path("recoverPassword") & authTokenExpirableCommand[PasswordRecoverCommand, FinishPasswordRecover]()) { cmd ⇒
          onSuccess(passwordRecoveryService.finishRecovery(cmd._1.email, cmd._2.newPass).flatMap(authService.getStatus)) { authStatus ⇒
            respondWithAuth(authStatus) {
              complete(authStatus)
            }
          }
        } ~ (path("verifyEmail") & authTokenCommand[EmailVerifyCommand, EmailVerifyToken]) { cmd ⇒
          onSuccess(emailVerifierService.verify(cmd._1.email)) {
            complete(emptyResponse)
          }
        } ~ (path("requestEmailVerify") & userRequired) { s ⇒
          complete(
            emailVerifierService.startVerify(s.userId)
              .map(_ ⇒ emptyResponse)
          )
        } ~ (path("checkEmailAvailability") & entity(as[EmailCheckAvailability])) { eca ⇒
          onSuccess(service.isEmailRegistered(eca.email)) {
            case true  ⇒ complete(emptyResponse)
            case false ⇒ failWith(AuthError.UserAlreadyRegistered)
          }
        } ~ (path("startEmailChange") & userRequired & entity(as[StartEmailChange])) { (status, cmd) ⇒
          onSuccess(emailChangeService.start(status.userId, cmd.email)) {
            complete(emptyResponse)
          }
        } ~ (path("finishEmailChange") & authTokenExpirableCommand[ChangeEmailCommand, FinishEmailChange]()) { cmd ⇒
          onSuccess(emailChangeService.finish(cmd._1.userId, cmd._1.newEmail).flatMap(service.getStatus)){ authStatus ⇒
            respondWithAuth(authStatus) {
              complete(authStatus)
            }
          }
        }
      }
    }

  def authTokenCommand[T: Decoder, Y <: TokenCommand: Decoder]: Directive1[(T, Y)] = entity(as[Y]).flatMap { tokenHolder ⇒
    crypt.decrypt[T](tokenHolder.token) match {
      case Success(tokenCommand) ⇒ provide((tokenCommand, tokenHolder))
      case _                     ⇒ failWith(AuthError.WrongToken)
    }
  }

  def authTokenExpirableCommand[T <: ExpirableCommand: Decoder, Y <: TokenCommand: Decoder](millisToLive: Long = 86400000): Directive1[(T, Y)] =
    authTokenCommand[T, Y].flatMap { cmd ⇒
      if (cmd._1.isExpired(millisToLive)) {
        failWith(AuthError.TardyToken)
      } else {
        provide(cmd)
      }
    }
}
