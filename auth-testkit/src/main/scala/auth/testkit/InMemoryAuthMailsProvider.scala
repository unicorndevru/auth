package auth.testkit

import auth.api.{ AuthMailsService, AuthMailsServiceProvider }
import auth.protocol.AuthUserId

trait InMemoryAuthMailsProvider extends AuthMailsServiceProvider {
  override lazy val authMailsService: AuthMailsService = InMemoryAuthMailsProvider
}

object InMemoryAuthMailsProvider extends AuthMailsService {
  var mails: Seq[(String, AuthUserId, Any)] = Seq.empty

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