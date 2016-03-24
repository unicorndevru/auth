package auth.handlers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import auth.AuthServicesComposition
import auth.directives.AuthDirectives
import auth.protocol._
import org.apache.commons.validator.routines.EmailValidator
import utils.http.directives.ValidationDirectives
import utils.http.json.JsonMarshallingContext

import scala.concurrent.ExecutionContext

class AuthHandler(val composition: AuthServicesComposition)(implicit ec: ExecutionContext, mat: Materializer) extends AuthHandlerJson with AuthDirectives with ValidationDirectives {

  import composition.{ authService ⇒ service, emailPasswordServices, userIdentityService, checkPasswordService }

  override val authParams = composition.authParams

  def authorize(cmd: AuthorizeCommand)(implicit jsonCtx: JsonMarshallingContext) =
    onSuccess(service.authorize(cmd)) {
      case Some(s) ⇒
        respondWithAuth(s) {
          complete(s)
        }
      case None ⇒
        failWith(AuthError.InvalidCredentials)
    }

  def register(cmd: AuthorizeCommand)(implicit jsonCtx: JsonMarshallingContext) =
    onSuccess(service.register(cmd)) { s ⇒
      respondWithAuth(s) {
        complete(StatusCodes.Created → s)
      }
    }

  val route =
    (pathPrefix("auth") & extractJsonMarshallingContext) { implicit jsonCtx ⇒
      pathEndOrSingleSlash {
        (get & userRequired) { status ⇒
          complete(status)

        } ~ post {
          entity(as[AuthByToken])(authorize) ~ entity(as[AuthByCredentials])(authorize)

        } ~ delete {
          failWith(AuthError.Unauthorized)

        } ~ put {
          entity(as[AuthByToken])(register) ~ entity(as[AuthByCredentials]){ cmd ⇒
            onValid(
              cmd.email.trim.nonEmpty → AuthError.NonEmptyRequired.forField("email"),
              EmailValidator.getInstance().isValid(cmd.email) → AuthError.MalformedEmail.forField("email"),
              checkPasswordService.isStrongEnough(cmd.password) → AuthError.PasswordNotStrongEnough.forField("password")
            ) {
                register(cmd)
              }
          }
        }
      } ~
        new IdentitiesHandler(userIdentityService, authParams).route ~
        new AuthActionsHandler(composition.credentialsCommandCrypto, service, emailPasswordServices, checkPasswordService, authParams).route
    }

}