package auth.protocol

sealed trait AuthorizeCommand {
  val provider: String
}

case class AuthByToken(provider: String, token: String) extends AuthorizeCommand

case class AuthByCredentials(provider: String, email: String, password: String) extends AuthorizeCommand

object AuthorizeCommand {
  import io.circe._
  import io.circe.generic.semiauto._
  import io.circe.parse._
  import io.circe.syntax._

  implicit val ec: Decoder[AuthorizeCommand] = deriveFor[AuthorizeCommand].decoder
}