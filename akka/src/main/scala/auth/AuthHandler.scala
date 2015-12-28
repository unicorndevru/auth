package auth

import akka.http.scaladsl.model.{ HttpEntity, HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import auth.api.AuthDirectives
import auth.protocol._
import auth.services.AuthService
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto._

import scala.concurrent.{ Future, ExecutionContext }

class AuthHandler(service: AuthService)(implicit ec: ExecutionContext, mat: Materializer) extends CirceSupport with AuthDirectives {

  val NoContent = HttpResponse(StatusCodes.NoContent, entity = HttpEntity.Empty)

  implicit val authStatus: Encoder[AuthStatus] = deriveFor[AuthStatus].encoder

  val route =
    pathPrefix("auth") {
      pathEndOrSingleSlash {
        (get & userRequired) { status ⇒
          complete(status)
        } ~ (post & entity(as[AuthorizeCommand])) { cmd ⇒
          complete(service.authorize(cmd).flatMap {
            case Some(pid) ⇒
              Future.successful("ok") // TODO: make status, add jwt token
            case None ⇒
              Future.failed(AuthError.InvalidCredentials)
          })
        } ~ delete {
          // jwtAuthenticator.clean(authEnv.unauthorizedResponse(request))
          complete(StatusCodes.Unauthorized → "") // TODO: add body
        } ~ (put & entity(as[AuthorizeCommand])) { cmd ⇒
          service.register(cmd) // TODO: add status, add jwt token, return 201
          /*
      for {
        pid ← profileRegistrar.register(request.body)
        aStatus ← authStatus(pid, false)
      } yield jwtAuthenticator.authorize(pid)(Created(Json.toJson(aStatus)))
       */
          complete("???")
        }
      }
    }

}
