package psgr.auth.core.providers.facebook

import org.joda.time.format.DateTimeFormat
import play.api.libs.json.JsValue

case class FacebookProfileReader(me: JsValue) {
  import FacebookProfileReader._

  lazy val userId = (me \ Id).as[String]
  lazy val name = (me \ Name).as[String]
  lazy val firstName = (me \ FirstName).as[String]
  lazy val lastName = (me \ LastName).as[String]
  lazy val avatarUrl = (me \ Picture \ Data \ Url).asOpt[String]
  lazy val email = (me \ Email).asOpt[String]
  lazy val locale = (me \ Locale).asOpt[String]
  lazy val verified = (me \ Verified).asOpt[Boolean]
  lazy val birthday = (me \ Birthday).asOpt[String].map(DateTimeFormat.forPattern("mm/dd/yyyy").parseDateTime)

}

object FacebookProfileReader {
  val Id = "id"
  val FirstName = "first_name"
  val LastName = "last_name"
  val Name = "name"
  val Picture = "picture"
  val Email = "email"
  val AccessToken = "access_token"
  val Expires = "expires"
  val Data = "data"
  val Url = "url"
  val Locale = "locale"
  val Verified = "verified"
  val Birthday = "birthday"
}