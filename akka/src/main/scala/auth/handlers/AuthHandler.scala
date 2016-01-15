package auth.handlers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import auth.AuthServicesComposition
import auth.directives.{ AuthDirectives, AuthParams }
import auth.api.UserIdentitiesService
import auth.protocol._
import auth.providers.email.EmailPasswordServices
import auth.services.AuthService
import de.heikoseeberger.akkahttpcirce.CirceSupport

import scala.concurrent.ExecutionContext

class AuthHandler(val composition: AuthServicesComposition)(implicit ec: ExecutionContext, mat: Materializer) extends CirceSupport with AuthDirectives with AuthCirceEncoders with AuthCirceDecoders {

  import composition.{ authService ⇒ service }
  import composition.{ userIdentityService, emailPasswordServices }
  override val authParams = composition.authParams

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
      } ~ new IdentitiesHandler(userIdentityService, authParams).route ~ new AuthActionsHandler(composition.credentialsCommandCrypto, service, emailPasswordServices, authParams).route
    }

}