package auth.mongo.users

import java.time.Instant

import auth.protocol.AuthUserId

case class SimpleUser(
  id:          AuthUserId,
  firstName:   Option[String],
  lastName:    Option[String],
  fullName:    Option[String],
  email:       Option[String] = None,
  avatarUrl:   Option[String] = None,
  locale:      Option[String] = None,
  dateCreated: Instant,
  lastUpdated: Instant
)