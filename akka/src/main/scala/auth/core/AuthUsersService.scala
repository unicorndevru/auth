package auth.core

import auth.protocol.AuthUserId

import scala.concurrent.Future

trait AuthUsersService {
  def setEmail(id: AuthUserId, email: String, avatar: Option[String]): Future[AuthUserId]

  def create(cmd: CreateUser): Future[AuthUserId]

  def findEmail(id: AuthUserId): Future[Option[String]]
}

case class CreateUser(
  firstName: Option[String],
  lastName:  Option[String],
  fullName:  Option[String],
  avatarUrl: Option[String],
  locale:    Option[String],

  email: Option[String]
)
