package auth.protocol

case class CheckEmailChangeToken(token: String) extends TokenCommand

case class FinishEmailChange(token: String) extends TokenCommand

case class StartEmailChange(email: String)
