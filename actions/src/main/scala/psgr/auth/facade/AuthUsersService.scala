package psgr.auth.facade

import psgr.auth.protocol.AuthUserId
import psgr.expander.protocol.MetaRef

import scala.concurrent.Future

trait AuthUsersService {
  type UserDto

  def toRef(id: AuthUserId): MetaRef[UserDto]

  def setEmail(id: AuthUserId, email: String, avatar: Option[String]): Future[AuthUserId]

  def create(cmd: CreateUser): Future[AuthUserId]

  def findEmail(id: AuthUserId): Future[Option[String]]
}

case class CreateUser(
  firstName:  String,
  lastName:   String,
  fullName:   String,
  avatarUrl:  Option[String],
  facebookId: Option[String],
  locale:     Option[String],

  email: Option[String]
)
