package psgr.auth.protocol

import play.api.libs.json.Json

case class AuthIdentityId(id: String, provider: String)

object AuthIdentityId {
  implicit def format = Json.format[AuthIdentityId]
}