package psgr.auth.protocol

import play.api.libs.json.Json

case class AuthStatus(
  user:       AuthUserId,
  roles:      Seq[String],
  isSwitched: Option[Boolean]
)

object AuthStatus {
  implicit val format = Json.format[AuthStatus]
}