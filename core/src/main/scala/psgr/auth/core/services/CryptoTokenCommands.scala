package psgr.auth.core.services

import play.api.libs.json.Json
import psgr.auth.protocol.AuthUserId

trait ExpirableCommand {

  val timestamp: Long

  def isExpired(lifetime: Long): Boolean = System.currentTimeMillis() - timestamp > lifetime
}

private[auth] case class PasswordRecoverCommand(
  email:     String,
  id:        AuthUserId,
  timestamp: Long       = System.currentTimeMillis
) extends ExpirableCommand

object PasswordRecoverCommand {
  implicit val format = Json.format[PasswordRecoverCommand]
}

private[auth] case class EmailVerifyCommand(email: String)

object EmailVerifyCommand {
  implicit val format = Json.format[EmailVerifyCommand]
}

private[auth] case class ChangeEmailCommand(
  newEmail:  String,
  userId:    AuthUserId,
  timestamp: Long       = System.currentTimeMillis()
) extends ExpirableCommand

object ChangeEmailCommand {
  implicit val format = Json.format[ChangeEmailCommand]
}