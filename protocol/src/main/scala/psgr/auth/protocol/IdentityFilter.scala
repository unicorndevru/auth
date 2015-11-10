package psgr.auth.protocol

import play.api.libs.json.Json

case class IdentityFilter(
  profileId: Option[AuthUserId] = None,
  email:     Option[String]     = None
)

object IdentityFilter {
  implicit val format = Json.format[IdentityFilter]
}
