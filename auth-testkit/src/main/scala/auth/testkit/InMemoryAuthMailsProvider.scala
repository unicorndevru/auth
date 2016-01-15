package auth.testkit

import auth.api.{ AuthMailsService, AuthMailsServiceProvider }
import auth.protocol.AuthUserId

trait InMemoryAuthMailsProvider extends AuthMailsServiceProvider {
  override lazy val authMailsService: AuthMailsService = InMemoryAuthMailsProvider
}

object InMemoryAuthMailsProvider extends AuthMailsService {
  var mails: Seq[(String, AuthUserId, Any)] = Seq.empty

  def reset() = mails = Seq.empty

  def mailsAsTuple2(): Seq[(String, AuthUserId)] = mails.map{ k ⇒ (k._1, k._2) }

  def getMailsById(id: AuthUserId): Seq[(String, AuthUserId, Any)] = mails.filter{ mail ⇒ mail._2.id == id.id }

  def getMailsByIdAndReason(id: AuthUserId, reason: String): Seq[(String, AuthUserId, Any)] =
    mails.filter{ mail ⇒ mail._2.id == id.id && mail._1 == reason }

  override def newPassword(id: AuthUserId, newPassword: String) =
    mails = mails :+ ("newPassword", id, ())

  override def emailVerify(id: AuthUserId, email: String, token: String) =
    mails = mails :+ ("emailVerify", id, (email, token))

  override def passwordRecoverNotify(id: AuthUserId) =
    mails = mails :+ ("passwordRecoverNotify", id, ())

  override def passwordRecover(id: AuthUserId, token: String) =
    mails = mails :+ ("passwordRecover", id, token)

  override def changeEmail(id: AuthUserId, newEmail: String, token: String) =
    mails = mails :+ ("changeEmail", id, (newEmail, token))

}