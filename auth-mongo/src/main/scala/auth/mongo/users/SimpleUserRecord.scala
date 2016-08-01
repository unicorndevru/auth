package auth.mongo.users

import java.time.Instant

import reactivemongo.bson.{ BSONDateTime, BSONReader, BSONWriter, Macros }

private[mongo] case class SimpleUserRecord(
  _id:         String,
  firstName:   Option[String],
  lastName:    Option[String],
  fullName:    Option[String],
  email:       Option[String] = None,
  avatarUrl:   Option[String] = None,
  locale:      Option[String] = None,
  dateCreated: Instant        = Instant.now(),
  lastUpdated: Instant        = Instant.now()
)

object SimpleUserRecord {
  implicit val dateTimeHandler = new BSONReader[BSONDateTime, Instant] with BSONWriter[Instant, BSONDateTime] {
    override def read(bson: BSONDateTime): Instant = Instant.ofEpochMilli(bson.value)

    override def write(t: Instant): BSONDateTime = BSONDateTime(t.toEpochMilli)
  }

  implicit val handler = Macros.handler[SimpleUserRecord]
}