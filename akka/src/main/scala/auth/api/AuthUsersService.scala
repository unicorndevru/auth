package auth.api

import auth.protocol.AuthUserId

import scala.concurrent.Future

trait AuthUsersService {
  def setEmail(id: AuthUserId, email: String, avatar: Option[String]): Future[AuthUserId]

  def create(cmd: CreateUser): Future[AuthUserId]

  def findEmail(id: AuthUserId): Future[Option[String]]

  def getRoles(id: AuthUserId): Future[Seq[String]]

  def getRolePermissions(role: String): Future[Seq[String]] = Future.successful(Seq(role))
}

case class CreateUser(
  firstName: Option[String],
  lastName:  Option[String],
  fullName:  Option[String],
  avatarUrl: Option[String],
  locale:    Option[String],

  email: Option[String]
)
