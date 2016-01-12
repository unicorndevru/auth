package auth.handlers

import auth.protocol._
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto._

trait AuthCirceDecoders {
  implicit val authByTokenDecoder: Decoder[AuthByToken] = deriveFor[AuthByToken].decoder
  implicit val authByCredentialsDecoder: Decoder[AuthByCredentials] = deriveFor[AuthByCredentials].decoder

  implicit val switchUserCommand: Decoder[SwitchUserCommand] = deriveFor[SwitchUserCommand].decoder
  implicit val passwordChange: Decoder[PasswordChange] = deriveFor[PasswordChange].decoder
  implicit val startPasswordRecovery: Decoder[StartPasswordRecover] = deriveFor[StartPasswordRecover].decoder

  implicit val emailCheckAvailability: Decoder[EmailCheckAvailability] = deriveFor[EmailCheckAvailability].decoder
}
