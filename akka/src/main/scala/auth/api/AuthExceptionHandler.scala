package auth.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ Directives, ExceptionHandler }
import auth.protocol.AuthError
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe._

object AuthExceptionHandler extends CirceSupport {

  implicit val authErrorEncoder: Encoder[AuthError] = Encoder.instance[AuthError](err ⇒
    Json.obj("code" → Json.string(err.code), "desc" → Json.string(err.desc)))

  private def errorResponse(e: AuthError) =
    Directives.complete(StatusCodes.custom(e.statusCode, e.desc) → e)

  val generic = ExceptionHandler {
    case e: AuthError ⇒
      errorResponse(e)
  }
}
