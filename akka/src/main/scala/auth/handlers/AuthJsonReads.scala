package auth.handlers

import auth.api.{ PasswordRecoverCommand, EmailVerifyCommand, ChangeEmailCommand }
import auth.protocol._
import play.api.libs.json.{ Json, Reads }

trait AuthJsonReads {
  implicit val authIdReads = Reads[AuthUserId](_.validate[String].map(AuthUserId))

  implicit val authByTokenDecoder: Reads[AuthByToken] = Json.reads[AuthByToken]
  implicit val authByCredentialsDecoder: Reads[AuthByCredentials] = Json.reads[AuthByCredentials]

  implicit val switchUserCommand: Reads[SwitchUserCommand] = Json.reads[SwitchUserCommand]
  implicit val passwordChange: Reads[PasswordChange] = Json.reads[PasswordChange]
  implicit val startPasswordRecovery: Reads[StartPasswordRecover] = Json.reads[StartPasswordRecover]

  implicit val emailCheckAvailability: Reads[EmailCheckAvailability] = Json.reads[EmailCheckAvailability]
  implicit val emailVerifyToken: Reads[EmailVerifyToken] = Json.reads[EmailVerifyToken]
  implicit val checkEmailChangeToken: Reads[CheckEmailChangeToken] = Json.reads[CheckEmailChangeToken]
  implicit val startEmailChange: Reads[StartEmailChange] = Json.reads[StartEmailChange]
  implicit val finishEmailChange: Reads[FinishEmailChange] = Json.reads[FinishEmailChange]
  implicit val finishPasswordRecover: Reads[FinishPasswordRecover] = Json.reads[FinishPasswordRecover]
  implicit val checkPasswordRecoverToken: Reads[CheckPasswordRecoverToken] = Json.reads[CheckPasswordRecoverToken]

  implicit val changeEmailCommand: Reads[ChangeEmailCommand] = Json.reads[ChangeEmailCommand]
  implicit val emailVerifyCommand: Reads[EmailVerifyCommand] = Json.reads[EmailVerifyCommand]
  implicit val passwordRecoverCommand: Reads[PasswordRecoverCommand] = Json.reads[PasswordRecoverCommand]

}
