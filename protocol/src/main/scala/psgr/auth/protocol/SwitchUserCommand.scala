package psgr.auth.protocol

import play.api.libs.json.Json

case class SwitchUserCommand(userId: AuthUserId)

object SwitchUserCommand {
  implicit val fmt = Json.format[SwitchUserCommand]
}
