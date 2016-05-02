package auth.mongo.users

import java.util.UUID

import auth.api.{ AuthUsersService, CreateUser }
import auth.mongo.AuthDao
import auth.protocol.AuthUserId
import reactivemongo.api.DB

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SimpleMongoUserService(db: DB) extends AuthDao[SimpleUserRecord, String](db, "users") with AuthUsersService {

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
    updateById(id.id, $doc("$set" → $doc("email" → email, "avatarUrl" → avatar))).map(_ ⇒ id)
  }

  override def getRoles(id: AuthUserId) = Future.successful(Set.empty)

  override def findEmail(id: AuthUserId) =
    findById(id.id).map(_.flatMap(_.email))

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
    insert(u).map(_ ⇒ recordToProtocol(u).id)
  }

}
