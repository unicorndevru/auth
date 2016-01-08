package auth.protocol.identities

case class AuthIdentity(
  id:              String,
  identityId:      AuthIdentityId,
  email:           Option[String],
  isEmailVerified: Option[Boolean]
)