package auth.api

import auth.protocol.AuthUserId

sealed trait ExpirableCommand {

  val timestamp: Long

  def isExpired(lifetime: Long): Boolean = System.currentTimeMillis() - timestamp > lifetime
}

case class PasswordRecoverCommand(
  email:     String,
  id:        AuthUserId,
  timestamp: Long       = System.currentTimeMillis
) extends ExpirableCommand

case class EmailVerifyCommand(email: String)

case class ChangeEmailCommand(
  newEmail:  String,
  userId:    AuthUserId,
  timestamp: Long       = System.currentTimeMillis()
) extends ExpirableCommand