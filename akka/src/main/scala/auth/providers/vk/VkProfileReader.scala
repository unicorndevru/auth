package auth.providers.vk

import play.api.libs.json.JsValue

case class VkProfileReader(me: JsValue) {
  private lazy val response = (me \ "response").apply(0)
  lazy val userId = (response \ "uid").as[Long]
  lazy val firstName = (response \ "first_name").asOpt[String]
  lazy val lastName = (response \ "last_name").asOpt[String]
  lazy val avatarURL = (response \ "photo").asOpt[String]
}
