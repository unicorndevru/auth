package auth

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import auth.api.{ AuthCirceDecoders, AuthCirceEncoders, AuthDirectives }
import auth.protocol._
import auth.services.AuthService
import de.heikoseeberger.akkahttpcirce.CirceSupport

import scala.concurrent.ExecutionContext

class AuthHandler(service: AuthService)(implicit ec: ExecutionContext, mat: Materializer) extends CirceSupport with AuthDirectives with AuthCirceEncoders with AuthCirceDecoders {

  val route =
    pathPrefix("auth") {
      pathEndOrSingleSlash {
        (get & userRequired) { status ⇒
          complete(status)

        } ~ post {
          entity(as[AuthByToken]) {
            cmd ⇒
              onSuccess(service.authorize(cmd)) {

                case Some(s) ⇒
                  respondWithAuth(s) {
                    complete(s)
                  }
                case None ⇒
                  failWith(AuthError.InvalidCredentials)

              }

          } ~ entity(as[AuthByCredentials]) {
            cmd ⇒
              onSuccess(service.authorize(cmd)) {

                case Some(s) ⇒
                  respondWithAuth(s) {
                    complete(s)
                  }
                case None ⇒
                  failWith(AuthError.InvalidCredentials)

              }
          }

        } ~ delete {
          failWith(AuthError.Unauthorized)

        } ~ put {
          entity(as[AuthByToken]) {
            cmd ⇒
              onSuccess(service.register(cmd)) { s ⇒
                respondWithAuth(s) {
                  complete(StatusCodes.Created → s)
                }
              }

          } ~ entity(as[AuthByCredentials]) {
            cmd ⇒
              onSuccess(service.register(cmd)) { s ⇒
                respondWithAuth(s) {
                  complete(StatusCodes.Created → s)
                }
              }
          }
        }
      }
    }

}
