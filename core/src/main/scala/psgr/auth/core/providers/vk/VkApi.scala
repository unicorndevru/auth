package psgr.auth.core.providers.vk

import javax.inject.Inject

import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import psgr.failures.JsonApiFailure
import psgr.auth.core.OAuth2Info

import scala.concurrent.{ ExecutionContext, Future }

class VkApi @Inject() (ws: WSClient) {

  val apiUrl = "https://api.vk.com/method/getProfiles?fields=uid,first_name,last_name,photo&access_token=%s"

  def getMe(info: OAuth2Info)(implicit ec: ExecutionContext): Future[VkProfileReader] = {
    ws.url(apiUrl.format(info.accessToken)).get().flatMap {
      response ⇒
        val json = response.json
        (json \ "error").asOpt[JsObject] match {
          case Some(error) ⇒
            val errorCode = (error \ "error_code").as[Int]
            val errorMsg = (error \ "error_msg").as[String]
            Future.failed(JsonApiFailure(
              400,
              "error_while_retrieving_vk_profile",
              s"Cannot retieve vk profile. error code = $errorCode, errorMsg = $errorMsg.",
              "auth"
            ))
          case _ ⇒ Future.successful(VkProfileReader(json))
        }
    }
  }

}
