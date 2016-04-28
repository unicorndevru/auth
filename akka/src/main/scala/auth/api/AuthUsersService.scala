package auth.api

import auth.protocol.AuthUserId
import play.api.libs.json.JsObject

import scala.concurrent.Future

trait AuthUsersService {
  def setEmail(id: AuthUserId, email: String, avatar: Option[String]): Future[AuthUserId]

  def create(cmd: CreateUser): Future[AuthUserId]

  def findEmail(id: AuthUserId): Future[Option[String]]

  def getRoles(id: AuthUserId): Future[Set[String]]

  def getRolePermissions(role: String): Future[Set[String]] = Future.successful(Set(role))
}

case class CreateUser(
  firstName: Option[String],
  lastName:  Option[String],
  fullName:  Option[String],
  avatarUrl: Option[String],
  locale:    Option[String],
  data:      Option[JsObject],

  email: Option[String]
)
