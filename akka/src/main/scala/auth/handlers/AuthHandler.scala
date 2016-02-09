package auth.handlers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import auth.AuthServicesComposition
import auth.directives.AuthDirectives
import auth.protocol._
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import scala.concurrent.ExecutionContext

class AuthHandler(val composition: AuthServicesComposition)(implicit ec: ExecutionContext, mat: Materializer) extends PlayJsonSupport with AuthDirectives with AuthJsonWrites with AuthJsonReads {

  import composition.{ authService ⇒ service, emailPasswordServices, userIdentityService }

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
    (handleExceptions(AuthExceptionHandler.generic) & pathPrefix("auth")) {
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