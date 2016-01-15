package auth.handlers

import auth.api.{ PasswordRecoverCommand, EmailVerifyCommand, ChangeEmailCommand }
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
  implicit val emailVerifyToken: Decoder[EmailVerifyToken] = deriveFor[EmailVerifyToken].decoder
  implicit val checkEmailChangeToken: Decoder[CheckEmailChangeToken] = deriveFor[CheckEmailChangeToken].decoder
  implicit val startEmailChange: Decoder[StartEmailChange] = deriveFor[StartEmailChange].decoder
  implicit val finishEmailChange: Decoder[FinishEmailChange] = deriveFor[FinishEmailChange].decoder
  implicit val finishPasswordRecover: Decoder[FinishPasswordRecover] = deriveFor[FinishPasswordRecover].decoder
  implicit val checkPasswordRecoverToken: Decoder[CheckPasswordRecoverToken] = deriveFor[CheckPasswordRecoverToken].decoder

  implicit val changeEmailCommand: Decoder[ChangeEmailCommand] = deriveFor[ChangeEmailCommand].decoder
  implicit val emailVerifyCommand: Decoder[EmailVerifyCommand] = deriveFor[EmailVerifyCommand].decoder
  implicit val passwordRecoverCommand: Decoder[PasswordRecoverCommand] = deriveFor[PasswordRecoverCommand].decoder

}
