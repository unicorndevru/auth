package auth

import akka.http.scaladsl.model.{ HttpEntity, HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import auth.api.AuthDirectives
import auth.protocol.{ AuthByCredentials, AuthByToken, AuthorizeCommand }
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe._
import io.circe.generic.auto._
import io.circe.parse._
import io.circe.syntax._

import scala.concurrent.ExecutionContext

class AuthHandler(implicit ec: ExecutionContext, mat: Materializer) extends CirceSupport with AuthDirectives {

  val NoContent = HttpResponse(StatusCodes.NoContent, entity = HttpEntity.Empty)

  val route =
    pathPrefix("auth") {
      pathEndOrSingleSlash {
        get {
          //  authStatusResp(request.userId)
          complete("???")
        } ~ (post & entity(as[AuthorizeCommand])) { cmd ⇒
          /*
        profileAuthenticator.authorize(request.body).flatMap {
        case Some(pid) ⇒
          authStatus(pid, false)
            .map(_.ok)
            .map(jwtAuthenticator.authorize(pid))
        case None ⇒
          JsonApiFailure(400, "wrong_credentials", "Credentials you provide are not valid", "auth", Some(Json.toJson(request.body))).resultF
      }
         */
          complete("???")
        } ~ delete {
          // jwtAuthenticator.clean(authEnv.unauthorizedResponse(request))
          complete("???")
        } ~ (put & entity(as[AuthorizeCommand])) { cmd ⇒
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
