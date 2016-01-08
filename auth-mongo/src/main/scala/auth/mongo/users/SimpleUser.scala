package auth.mongo.users

import auth.protocol.AuthUserId
import org.joda.time.DateTime

case class SimpleUser(
  id:          AuthUserId,
  firstName:   Option[String],
  lastName:    Option[String],
  fullName:    Option[String],
  email:       Option[String] = None,
  avatarUrl:   Option[String] = None,
  locale:      Option[String] = None,
  dateCreated: DateTime,
  lastUpdated: DateTime
)