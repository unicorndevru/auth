package auth.testkit

import java.util.concurrent.atomic.AtomicInteger

import auth.api.{ AuthMailsService, AuthMailsServiceProvider }
import auth.protocol.AuthUserId

import scala.collection.concurrent.TrieMap

trait InMemoryAuthMailsProvider extends AuthMailsServiceProvider {
  override lazy val authMailsService: AuthMailsService = InMemoryAuthMailsProvider
}

object InMemoryAuthMailsProvider extends AuthMailsService {

  val ms = TrieMap.empty[Int, (String, AuthUserId, Any)]
  val i = new AtomicInteger(0)

  def push(r: String, u: AuthUserId, p: Any) = {
    ms.put(i.getAndIncrement(), (r, u, p))
  }

  def contains(reason: String, id: AuthUserId) = ms.exists(kv ⇒ kv._2._1 == reason && kv._2._2 == id)

  def payload(reason: String, id: AuthUserId) = ms.find(kv ⇒ kv._2._1 == reason && kv._2._2 == id).map(_._2._3)

  def getMailsByIdAndReason(id: AuthUserId, reason: String): Seq[(String, AuthUserId, Any)] = ms.values.filter(v ⇒ v._1 == reason && v._2 == id).toSeq

  def getMailsById(id: AuthUserId): Seq[(String, AuthUserId, Any)] = ms.values.filter(_._2 == id).toSeq

  override def newPassword(id: AuthUserId, newPassword: String) =
    push("newPassword", id, ())

  override def emailVerify(id: AuthUserId, email: String, token: String) =
    push("emailVerify", id, (email, token))

  override def passwordRecoverNotify(id: AuthUserId) =
    push("passwordRecoverNotify", id, ())

  override def passwordRecover(id: AuthUserId, token: String) =
    push("passwordRecover", id, token)

  override def changeEmail(id: AuthUserId, newEmail: String, token: String) =
    push("changeEmail", id, (newEmail, token))

}