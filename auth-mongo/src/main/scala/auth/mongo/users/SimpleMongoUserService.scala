package auth.mongo.users

import java.util.UUID

import auth.api.{ AuthUsersService, CreateUser }
import auth.protocol.AuthUserId
import reactivemongo.api.DB
import reactivemongo.extensions.dao.BsonDao
import reactivemongo.extensions.dsl.BsonDsl._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SimpleMongoUserService(db: DB) extends AuthUsersService {

  object dao extends BsonDao[SimpleUserRecord, String](db, "users")

  def recordToProtocol(r: SimpleUserRecord) = SimpleUser(
    id = AuthUserId(r._id),
    firstName = r.firstName,
    lastName = r.lastName,
    fullName = r.fullName,
    email = r.email,
    avatarUrl = r.avatarUrl,
    locale = r.locale,
    dateCreated = r.dateCreated,
    lastUpdated = r.lastUpdated
  )

  override def setEmail(id: AuthUserId, email: String, avatar: Option[String]) = {
    dao.updateById(id.id, $doc("email" → email, "avatarUrl" → avatar)).filter(_.ok).map(_ ⇒ id)
  }

  override def getRoles(id: AuthUserId) = Future.successful(Set.empty)

  override def findEmail(id: AuthUserId) =
    dao.findById(id.id).map(_.flatMap(_.email))

  override def create(cmd: CreateUser) = {
    val u = SimpleUserRecord(
      _id = UUID.randomUUID().toString,
      firstName = cmd.firstName,
      lastName = cmd.lastName,
      fullName = cmd.fullName,
      avatarUrl = cmd.avatarUrl,
      email = cmd.email,
      locale = cmd.locale
    )
    dao.insert(u).filter(_.ok).map(_ ⇒ recordToProtocol(u).id)
  }

}
