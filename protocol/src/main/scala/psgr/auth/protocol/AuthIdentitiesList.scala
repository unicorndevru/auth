package psgr.auth.protocol

import play.api.libs.json.Json

case class AuthIdentitiesList(
  items: Seq[AuthIdentity]
)

object AuthIdentitiesList {
  implicit val format = Json.format[AuthIdentitiesList]
}