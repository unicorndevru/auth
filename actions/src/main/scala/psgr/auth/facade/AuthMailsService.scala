package psgr.auth.facade

import psgr.auth.protocol.AuthUserId

trait AuthMailsService {
  def newPassword(id: AuthUserId, newPassword: String): Unit

  def changeEmail(id: AuthUserId, newEmail: String, token: String): Unit

  def emailVerify(id: AuthUserId, email: String, token: String): Unit

  def passwordRecoverNotify(id: AuthUserId): Unit

  def passwordRecover(id: AuthUserId, token: String): Unit
}
