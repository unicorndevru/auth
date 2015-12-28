package auth.protocol

case class AuthIdentity(
  id:              String,
  identityId:      AuthIdentityId,
  email:           Option[String],
  isEmailVerified: Option[Boolean]
)