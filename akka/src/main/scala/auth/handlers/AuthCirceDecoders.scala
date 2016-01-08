package auth.handlers

import auth.protocol.{AuthByCredentials, AuthByToken, PasswordChange, SwitchUserCommand}
import io.circe._
import io.circe.generic.semiauto._

trait AuthCirceDecoders {
  implicit val authByTokenDecoder: Decoder[AuthByToken] = deriveFor[AuthByToken].decoder
  implicit val authByCredentialsDecoder: Decoder[AuthByCredentials] = deriveFor[AuthByCredentials].decoder

  implicit val switchUserCommand: Decoder[SwitchUserCommand] = deriveFor[SwitchUserCommand].decoder
  implicit val passwordChange: Decoder[PasswordChange] = deriveFor[PasswordChange].decoder
}
