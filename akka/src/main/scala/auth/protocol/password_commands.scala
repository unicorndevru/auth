package auth.protocol

case class PasswordChange(oldPass: Option[String], newPass: String)

case class StartPasswordRecover(email: String)

case class FinishPasswordRecover(token: String, newPass: String) extends TokenCommand

case class CheckPasswordRecoverToken(token: String) extends TokenCommand