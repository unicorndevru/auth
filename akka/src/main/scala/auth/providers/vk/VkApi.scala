package auth.providers.vk

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, Uri }
import akka.stream.Materializer
import auth.data.identity.OAuth2Info
import auth.protocol.AuthError.VkProfileRetrievingError
import play.api.libs.json.{ JsObject, Json }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class VkApi(implicit system: ActorSystem, mat: Materializer) {

  val apiUrl = "https://api.vk.com/method/getProfiles?fields=uid,first_name,last_name,photo&access_token=%s"

  def getMe(info: OAuth2Info): Future[VkProfileReader] = {
    Http(system).singleRequest(HttpRequest(
      uri = Uri(apiUrl.format(info.accessToken))
    )).flatMap(_.entity.dataBytes.runReduce(_ ++ _))
      .map(_.utf8String)
      .map(Json.parse)
      .flatMap { json ⇒
        (json \ "error").asOpt[JsObject] match {
          case Some(error) ⇒
            val errorCode = (error \ "error_code").as[Int]
            val errorMsg = (error \ "error_msg").as[String]
            Future.failed(VkProfileRetrievingError(errorCode, errorMsg))
          case _ ⇒ Future.successful(VkProfileReader(json))
        }
      }
  }

}
