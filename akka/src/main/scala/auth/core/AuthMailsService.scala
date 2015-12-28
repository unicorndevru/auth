package auth.core

import akka.event.slf4j.Logger
import auth.protocol.AuthUserId

trait AuthMailsService {
  def newPassword(id: AuthUserId, newPassword: String): Unit

  def changeEmail(id: AuthUserId, newEmail: String, token: String): Unit

  def emailVerify(id: AuthUserId, email: String, token: String): Unit

  def passwordRecoverNotify(id: AuthUserId): Unit

  def passwordRecover(id: AuthUserId, token: String): Unit
}

object LoggingAuthMailsService extends AuthMailsService {
  val log = Logger("auth-mails-noop")

  override def newPassword(id: AuthUserId, newPassword: String) =
    log.info(s"Sent new password $id: $newPassword")

  override def emailVerify(id: AuthUserId, email: String, token: String) =
    log.info(s"Sent email verify $id: $email, $token")

  override def passwordRecoverNotify(id: AuthUserId) =
    log.info(s"Sent password recover notify: $id")

  override def passwordRecover(id: AuthUserId, token: String) =
    log.info(s"Sent password recover $id: $token")

  override def changeEmail(id: AuthUserId, newEmail: String, token: String) =
    log.info(s"Sent email change $id: $newEmail $token")
}