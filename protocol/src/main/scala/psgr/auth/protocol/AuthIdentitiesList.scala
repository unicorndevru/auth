package psgr.auth.protocol

import play.api.libs.json.Json
import psgr.expander.protocol.{ MetaId, MetaItems, MetaRef }

case class AuthIdentitiesList(
  meta: AuthIdentitiesList.Id = AuthIdentitiesList.metaId,

  items: Seq[AuthIdentity]
) extends MetaItems[AuthIdentity]

object AuthIdentitiesList {

  type Id = MetaId[AuthIdentitiesList]
  type Ref = MetaRef[AuthIdentitiesList]

  val metaId = MetaId[AuthIdentitiesList]("/api/auth/identities", 1)

  implicit val format = Json.format[AuthIdentitiesList]
}