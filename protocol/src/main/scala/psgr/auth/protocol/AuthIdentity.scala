package psgr.auth.protocol

import play.api.libs.json.Json

case class AuthIdentity(
  id:              String,
  identityId:      AuthIdentityId,
  email:           Option[String],
  isEmailVerified: Option[Boolean]
)

object AuthIdentity {
  implicit val format = Json.format[AuthIdentity]
}