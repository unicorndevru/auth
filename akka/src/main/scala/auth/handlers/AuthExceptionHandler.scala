package auth.handlers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ Directives, ExceptionHandler }
import auth.protocol.AuthError
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.{ Json, OWrites, Writes }

object AuthExceptionHandler extends PlayJsonSupport {

  implicit val authErrorEncoder: Writes[AuthError] = OWrites[AuthError](err ⇒
    Json.obj("code" → err.code, "desc" → err.desc))

  private def errorResponse(e: AuthError) =
    Directives.complete(StatusCodes.custom(e.statusCode, e.desc) → e)

  val generic = ExceptionHandler {
    case e: AuthError ⇒
      errorResponse(e)
  }
}
