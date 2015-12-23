package psgr.auth.core.providers.google

import javax.inject.Inject

import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import psgr.auth.core.identity.OAuth2Info
import psgr.failures.JsonApiFailure

import scala.concurrent.{ ExecutionContext, Future }

class GoogleApi @Inject() (ws: WSClient) {

  val apiUrl = "https://www.googleapis.com/plus/v1/people/me?access_token=%s"

  def getMe(info: OAuth2Info)(implicit ec: ExecutionContext): Future[GoogleProfileReader] = {
    ws.url(apiUrl.format(info.accessToken)).get().flatMap {
      response ⇒
        val json = response.json
        (json \ "error").asOpt[JsObject] match {
          case Some(error) ⇒
            val errorCode = (error \ "error_code").as[Int]
            val errorMsg = (error \ "error_msg").as[String]
            Future.failed(JsonApiFailure(
              400,
              "error_while_retrieving_google_profile",
              s"Cannot retieve google profile. error code = $errorCode, errorMsg = $errorMsg.",
              "auth"
            ))
          case _ ⇒ Future.successful(GoogleProfileReader(json))
        }
    }
  }
}