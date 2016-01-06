package auth

import auth.core.{ AuthUsersService, CreateUser }
import auth.protocol.AuthUserId

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TestAuthUsersService extends AuthUsersService {

  var users = TrieMap[AuthUserId, (CreateUser, Seq[String])]()

  var id = 0

  override def setEmail(id: AuthUserId, email: String, avatar: Option[String]) = {
    val u = users(id)._1
    users(id) = users(id).copy(_1 = u.copy(email = Some(email)))
    Future.successful(id)
  }

  override def findEmail(id: AuthUserId) = Future.successful(users.get(id).map(_._1).flatMap(_.email))

  override def create(cmd: CreateUser) = {
    id += 1
    val aid = AuthUserId("user-" + id.toString)
    users += (aid → (cmd, cmd.email.fold(Seq.empty[String])(_.split(',').toSeq)))
    Future.successful(aid)
  }

  override def getRoles(id: AuthUserId) = Future(users(id)._2)
}
