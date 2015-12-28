package auth.protocol

sealed abstract class AuthError(val code: String, val desc: String, val statusCode: Int = 500) extends Throwable {
  override def getMessage = s"Auth Error $code($statusCode): $desc"
}

object AuthError {

  object Unauthorized extends AuthError("auth.unauthorized", "Not authenticated", 401)

  object WrongPassword extends AuthError("auth.wrongPassword", "Wrong password", 401)

  object UserIdNotFound extends AuthError("auth.userIdNotFound", "Corrupted identity", 401)

  object WrongAuthObject extends AuthError("auth.wrongAuthorizeObject", "Cannot authorize with provided payload", 401)

  object InvalidCredentials extends AuthError("auth.invalidCredentials", "Credentials you provide are not valid", 401)

  object DuplicateIdentities extends AuthError("auth.cannotCreateIdentity", "Cannot create user identity: identity already exists", 409)

  object UserHaveNoEmails extends AuthError("auth.userHaveNoEmails", "Cannot perform action: user have no emails", 412)

  object UserEmailNotVerified extends AuthError("auth.emailNotVerified", "Cannot perform action: user have no verified emails", 412)

  object UserHaveNoPassword extends AuthError("auth.noPasswordIsSet", "Cannot perform action: user have no password identities", 412)

  object UserAlreadyRegistered extends AuthError("auth.userAlreadyRegistered", "User already registered", 409)

  object IdentityNotFound extends AuthError("auth.identityNotFound", "Cannot find identity by id", 404)

  case class ProviderNotFound(provider: String) extends AuthError("auth.providerNotFound", s"Cannot find provider $provider", 404)
}