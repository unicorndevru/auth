package auth.mongo.users

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONWriter, BSONDateTime, BSONReader, Macros }

private[mongo] case class SimpleUserRecord(
  _id:         String,
  firstName:   Option[String],
  lastName:    Option[String],
  fullName:    Option[String],
  email:       Option[String] = None,
  avatarUrl:   Option[String] = None,
  locale:      Option[String] = None,
  dateCreated: DateTime       = DateTime.now(),
  lastUpdated: DateTime       = DateTime.now()
)

object SimpleUserRecord {
  implicit val dateTimeHandler = new BSONReader[BSONDateTime, DateTime] with BSONWriter[DateTime, BSONDateTime] {
    override def read(bson: BSONDateTime): DateTime = new DateTime(bson.value)

    override def write(t: DateTime): BSONDateTime = BSONDateTime(t.getMillis)
  }

  implicit val handler = Macros.handler[SimpleUserRecord]
}