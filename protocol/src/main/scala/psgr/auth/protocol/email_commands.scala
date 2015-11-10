package psgr.auth.protocol

import play.api.libs.json.Json

case class EmailCheckAvailability(email: String)

object EmailCheckAvailability {
  implicit val format = Json.format[EmailCheckAvailability]
}

case class EmailVerifyToken(token: String) extends TokenCommand

object EmailVerifyToken {
  implicit val format = Json.format[EmailVerifyToken]
}