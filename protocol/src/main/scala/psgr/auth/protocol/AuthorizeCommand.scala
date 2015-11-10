package psgr.auth.protocol

import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait AuthorizeCommand {
  val provider: String
}

case class AuthByToken(provider: String, token: String) extends AuthorizeCommand

object AuthByToken {
  implicit val f = Json.format[AuthByToken]
}

case class AuthByCredentials(provider: String, email: String, password: String) extends AuthorizeCommand

object AuthByCredentials {
  implicit val f = Json.format[AuthByCredentials]
}

object AuthorizeCommand {
  implicit val format = new Format[AuthorizeCommand] {
    def reads(json: JsValue): JsResult[AuthorizeCommand] =
      json \ "provider" match {
        case JsDefined(JsString("email")) ⇒ AuthByCredentials.f.reads(json)
        case JsDefined(_: JsString)       ⇒ AuthByToken.f.reads(json)
        case _                            ⇒ JsError(ValidationError("Unknown Format"))
      }

    def writes(o: AuthorizeCommand): JsValue = o match {
      case token: AuthByToken       ⇒ Json.toJson(token)
      case login: AuthByCredentials ⇒ Json.toJson(login)
    }
  }
}