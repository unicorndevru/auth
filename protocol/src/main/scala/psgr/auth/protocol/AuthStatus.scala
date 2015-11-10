package psgr.auth.protocol

import play.api.libs.json.Json
import psgr.expander.protocol.{ MetaResource, MetaId, MetaRef }

case class AuthStatus(
  meta:       AuthStatus.Id          = AuthStatus.metaId,
  user:       MetaRef[Any],
  roles:      Seq[String],
  isSwitched: Option[Boolean],
  identities: AuthIdentitiesList.Ref
) extends MetaResource[AuthStatus]

object AuthStatus {

  type Id = MetaId[AuthStatus]
  type Ref = MetaRef[AuthStatus]

  val metaId = MetaId[AuthStatus]("/api/auth", 1)

  implicit val format = Json.format[AuthStatus]
}