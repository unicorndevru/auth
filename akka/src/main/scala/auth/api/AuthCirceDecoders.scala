package auth.api

import auth.protocol.{ SwitchUserCommand, AuthByCredentials, AuthByToken }
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.generic.semiauto._

trait AuthCirceDecoders {
  implicit val authByTokenDecoder: Decoder[AuthByToken] = deriveFor[AuthByToken].decoder
  implicit val authByCredentialsDecoder: Decoder[AuthByCredentials] = deriveFor[AuthByCredentials].decoder
  implicit val switchUserCommand: Decoder[SwitchUserCommand] = deriveFor[SwitchUserCommand].decoder
}
