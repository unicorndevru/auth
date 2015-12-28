package auth.protocol

case class EmailCheckAvailability(email: String)

case class EmailVerifyToken(token: String) extends TokenCommand