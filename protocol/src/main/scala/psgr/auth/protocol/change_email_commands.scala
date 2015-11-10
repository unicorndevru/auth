package psgr.auth.protocol

import play.api.libs.json.Json

case class CheckEmailChangeToken(token: String) extends TokenCommand

object CheckEmailChangeToken {
  implicit val format = Json.format[CheckEmailChangeToken]
}

case class FinishEmailChange(token: String) extends TokenCommand

object FinishEmailChange {
  implicit val format = Json.format[FinishEmailChange]
}

case class StartEmailChange(email: String)

object StartEmailChange {
  implicit val format = Json.format[StartEmailChange]
}