package psgr.auth.core.providers.google

import play.api.libs.json.JsValue

case class GoogleProfileReader(json: JsValue) {
  lazy val userId = (json \ "id").as[String]
  lazy val firstName = (json \ "name" \ "givenName").asOpt[String]
  lazy val lastName = (json \ "name" \ "familyName").asOpt[String]
  lazy val fullName = (json \ "displayName").asOpt[String]
  lazy val avatarURL = (json \ "image" \ "url").asOpt[String]

  private lazy val emailIndex = (json \ "emails" \\ "type").indexWhere(_.as[String] == "account")
  lazy val emailValue = if ((json \ "emails" \\ "value").isDefinedAt(emailIndex)) {
    (json \ "emails" \\ "value")(emailIndex).asOpt[String]
  } else {
    None
  }
}