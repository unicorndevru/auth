package psgr.auth.protocol

import play.api.libs.json.Json
import psgr.expander.protocol.{ MetaResource, MetaRef, MetaId }

case class AuthIdentity(
  meta: AuthIdentity.Id,

  id:              String,
  identityId:      AuthIdentityId,
  email:           Option[String],
  isEmailVerified: Option[Boolean]
) extends MetaResource[AuthIdentity]

object AuthIdentity {
  type Id = MetaId[AuthIdentity]
  type Ref = MetaRef[AuthIdentity]

  private val _id = MetaId[AuthIdentity]("/api/auth/identities", 1)

  implicit def pidToRef(id: String): Ref = pidToId(id)

  implicit def pidToId(id: String): Id = _id.appendHref(id)

  implicit val format = Json.format[AuthIdentity]
}