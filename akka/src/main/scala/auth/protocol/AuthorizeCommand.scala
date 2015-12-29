package auth.protocol

sealed trait AuthorizeCommand {
  val provider: String
}

case class AuthByToken(provider: String, token: String) extends AuthorizeCommand

case class AuthByCredentials(provider: String, email: String, password: String) extends AuthorizeCommand