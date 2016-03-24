package auth.protocol

import play.api.libs.json.JsObject
import utils.http.protocol.ApiError

object AuthError {

  object Unauthorized extends ApiError("auth.unauthorized", "Not authenticated", 401)

  object WrongPassword extends ApiError("auth.wrongPassword", "Wrong password", 406)

  object WrongToken extends ApiError("auth.wrongToken", "Wrong token", 406)

  object TardyToken extends ApiError("auth.tardyToken", "Token is tardy", 406)

  object UserIdNotFound extends ApiError("auth.userIdNotFound", "Corrupted identity", 401)

  object WrongAuthObject extends ApiError("auth.wrongAuthorizeObject", "Cannot authorize with provided payload", 406)

  object InvalidCredentials extends ApiError("auth.invalidCredentials", "Credentials you provide are not valid", 406)

  object DuplicateIdentities extends ApiError("auth.cannotCreateIdentity", "Cannot create user identity: identity already exists", 409)

  object UserHaveNoEmails extends ApiError("auth.userHaveNoEmails", "Cannot perform action: user have no emails", 412)

  object UserEmailNotVerified extends ApiError("auth.emailNotVerified", "Cannot perform action: user have no verified emails", 412)

  object UserHaveNoPassword extends ApiError("auth.noPasswordIsSet", "Cannot perform action: user have no password identities", 412)

  object UserAlreadyRegistered extends ApiError("auth.userAlreadyRegistered", "User already registered", 409)

  object IdentityNotFound extends ApiError("auth.identityNotFound", "Cannot find identity by id", 404)

  case class ProviderNotFound(provider: String) extends ApiError("auth.providerNotFound", s"Cannot find provider $provider", 404)

  case class JsonParseError(data: JsObject) extends ApiError("auth.parseError", s"Parse error ${data.toString()}", 400)

  object MalformedEmail extends ApiError("auth.malformedEmail", "Please provide real e-mail address", 400)

  object NonEmptyRequired extends ApiError("auth.nonEmptyRequired", "Please fill the field with appropriate data", 400)

  object PasswordNotStrongEnough extends ApiError("auth.passwordNotStrongEnough", "Password is not strong enough", 400)
}