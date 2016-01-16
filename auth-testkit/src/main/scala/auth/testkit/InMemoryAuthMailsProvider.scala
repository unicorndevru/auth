package auth.testkit

import akka.actor.{ Actor, Props, ActorSystem }
import auth.api.{ AuthMailsService, AuthMailsServiceProvider }
import auth.protocol.AuthUserId

import scala.concurrent.Future
import akka.pattern.ask
import scala.concurrent.duration._

trait InMemoryAuthMailsProvider extends AuthMailsServiceProvider {
  override lazy val authMailsService: AuthMailsService = InMemoryAuthMailsProvider
}

object InMemoryAuthMailsProvider extends AuthMailsService {

  private implicit val s = ActorSystem("test-mails")

  import s.dispatcher
  implicit val to = akka.util.Timeout(1 second)

  val mails = s.actorOf(Props(new Actor {
    override def receive = handle(Vector.empty)

    def handle(buff: Vector[(String, AuthUserId, Any)]): Receive = {
      case m: (String, AuthUserId, _) ⇒
        context.become(handle(buff :+ m))

      case (s: String, a: AuthUserId) ⇒
        sender() ! buff.find(p ⇒ p._1 == s && p._2 == a)

      case a: AuthUserId ⇒
        sender() ! buff.filter(_._2 == a)
    }
  }))

  def contains(reason: String, id: AuthUserId): Future[Boolean] = (mails ? (reason, id)).mapTo[Option[(String, AuthUserId, Any)]].map(_.isDefined)

  def payload(reason: String, id: AuthUserId): Future[Any] = (mails ? (reason, id)).mapTo[Option[(String, AuthUserId, Any)]].map(_.get._3)

  def getMailsByIdAndReason(id: AuthUserId, reason: String): Future[Seq[(String, AuthUserId, Any)]] = (mails ? id).mapTo[Vector[(String, AuthUserId, Any)]].map(_.filter(_._1 == reason).toSeq)

  def getMailsById(id: AuthUserId): Future[Seq[(String, AuthUserId, Any)]] = (mails ? id).mapTo[Vector[(String, AuthUserId, Any)]].map(_.toSeq)

  override def newPassword(id: AuthUserId, newPassword: String) =
    mails ! ("newPassword", id, ())

  override def emailVerify(id: AuthUserId, email: String, token: String) =
    mails ! ("emailVerify", id, (email, token))

  override def passwordRecoverNotify(id: AuthUserId) =
    mails ! ("passwordRecoverNotify", id, ())

  override def passwordRecover(id: AuthUserId, token: String) =
    mails ! ("passwordRecover", id, token)

  override def changeEmail(id: AuthUserId, newEmail: String, token: String) =
    mails ! ("changeEmail", id, (newEmail, token))

}