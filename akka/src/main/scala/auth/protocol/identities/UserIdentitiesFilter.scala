package auth.protocol.identities

import auth.protocol.AuthUserId

case class UserIdentitiesFilter(
  userId: Option[AuthUserId] = None,
  email:  Option[String]     = None
)
