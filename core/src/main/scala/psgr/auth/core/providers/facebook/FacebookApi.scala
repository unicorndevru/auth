package psgr.auth.core.providers.facebook

import javax.inject.Inject

import play.api.libs.json.JsObject
import play.api.libs.ws._
import psgr.auth.core.OAuth2Info
import psgr.failures.JsonApiFailure

import scala.concurrent.{ ExecutionContext, Future }

class FacebookApi @Inject() (wsc: WSClient) {

  val apiUrl = "https://graph.facebook.com/v2.3/me?fields=name,first_name,last_name,picture,email&return_ssl_resources=1&access_token=%s"

  def getMe(authInfo: OAuth2Info)(implicit ec: ExecutionContext): Future[FacebookProfileReader] = {
    wsc.url(apiUrl.format(authInfo.accessToken)).get().flatMap {
      response ⇒
        val json = response.json
        (json \ "error").asOpt[JsObject] match {
          case Some(error) ⇒
            val errorMsg = (error \ "message").as[String]
            val errorType = (error \ "type").as[String]
            val errorCode = (error \ "code").as[Int]
            Future.failed(JsonApiFailure(
              400,
              "error_while_retrieving_facebook_profile",
              s"Cannot retieve facebook profile. error code = $errorCode, errorMsg = $errorMsg, errorType = $errorType}",
              "auth"
            ))
          case _ ⇒ Future.successful(FacebookProfileReader(json))
        }
    }
  }

}
