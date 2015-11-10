package psgr.auth.protocol

import play.api.libs.json.Json

case class PasswordChange(oldPass: Option[String], newPass: String)

object PasswordChange {
  implicit val format = Json.format[PasswordChange]
}

case class StartPasswordRecover(email: String)

object StartPasswordRecover {
  implicit val format = Json.format[StartPasswordRecover]
}

case class FinishPasswordRecover(token: String, newPass: String) extends TokenCommand

object FinishPasswordRecover {
  implicit val format = Json.format[FinishPasswordRecover]
}

case class CheckPasswordRecoverToken(token: String) extends TokenCommand

object CheckPasswordRecoverToken {
  implicit val format = Json.format[CheckPasswordRecoverToken]
}