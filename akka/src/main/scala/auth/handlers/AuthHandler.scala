package auth.handlers

import akka.http.scaladsl.model.{ RemoteAddress, StatusCodes }
import akka.http.scaladsl.model.headers.`User-Agent`
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import auth.AuthServicesComposition
import auth.api.AuthRequestContext
import auth.directives.AuthDirectives
import auth.protocol._
import org.apache.commons.validator.routines.EmailValidator
import play.api.libs.json.JsObject
import utils.http.directives.ValidationDirectives

import scala.concurrent.ExecutionContext

class AuthHandler(val composition: AuthServicesComposition)(implicit ec: ExecutionContext, mat: Materializer) extends AuthHandlerJson with AuthDirectives with ValidationDirectives {

  import composition.{ authService ⇒ service, emailPasswordServices, userIdentityService, checkPasswordService, authEvents }

  override val authParams = composition.authParams

  def authenticate(cmd: AuthorizeCommand)(implicit ctx: AuthRequestContext) =
    onSuccess(service.authorize(cmd)) {
      case Some(s) ⇒
        respondWithAuth(s) {
          authEvents.userAuthenticated(s)
          complete(s)
        }
      case None ⇒
        failWith(AuthError.InvalidCredentials)
    }

  def register(cmd: AuthorizeCommand, data: Option[JsObject] = None)(implicit ctx: AuthRequestContext) =
    onSuccess(service.register(cmd, data)) { s ⇒
      respondWithAuth(s) {
        authEvents.userRegistered(s)
        complete(StatusCodes.Created → s)
      }
    }

  val route =
    (pathPrefix("auth") & extractAuthRequestContext) { implicit reqCtx ⇒
      pathEndOrSingleSlash {
        (get & userRequired) { status ⇒
          complete(status)

        } ~ post {
          entity(as[AuthByToken])(authenticate) ~ entity(as[AuthByCredentials])(authenticate)

        } ~ delete {
          failWith(AuthError.Unauthorized)

        } ~ put {
          entity(as[JsObject]){ json ⇒
            json.asOpt[AuthByToken] orElse json.asOpt[AuthByCredentials] match {
              case Some(cmd: AuthByToken) ⇒ register(cmd, Some(json))
              case Some(cmd: AuthByCredentials) ⇒
                onValid(
                  cmd.email.trim.nonEmpty → AuthError.NonEmptyRequired.forField("email"),
                  EmailValidator.getInstance().isValid(cmd.email) → AuthError.MalformedEmail.forField("email"),
                  checkPasswordService.isStrongEnough(cmd.password) → AuthError.PasswordNotStrongEnough.forField("password")
                ) {
                    register(cmd, Some(json))
                  }
              case _ ⇒ failWith(AuthError.WrongAuthObject)
            }
          }
        }
      } ~
        new IdentitiesHandler(userIdentityService, authParams).route ~
        new AuthActionsHandler(composition.credentialsCommandCrypto, service, emailPasswordServices, checkPasswordService, authParams).route
    }

}