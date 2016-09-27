package auth.providers.facebook

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, Uri }
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import auth.data.identity.OAuth2Info
import auth.protocol.AuthError
import play.api.libs.json.{ JsObject, Json }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class FacebookApi(implicit system: ActorSystem, mat: Materializer) {

  val apiUrl = "https://graph.facebook.com/v2.7/me?fields=name,first_name,last_name,picture,email,birthday&return_ssl_resources=1&access_token=%s"

  def getMe(authInfo: OAuth2Info): Future[FacebookProfileReader] = {
    Http(system).singleRequest(HttpRequest(
      uri = Uri(apiUrl.format(authInfo.accessToken))
    )).flatMap(_.entity.dataBytes.runWith(Sink.reduce[ByteString](_ ++ _))).flatMap {
      response ⇒
        val json = Json.parse(response.utf8String)
        (json \ "error").asOpt[JsObject] match {
          case Some(error) ⇒
            val errorMsg = (error \ "message").as[String]
            val errorType = (error \ "type").as[String]
            val errorCode = (error \ "code").as[Int]
            Future.failed(
              AuthError.FacebookProfileRetrievingError(errorCode, errorType, errorMsg)
            )
          case _ ⇒ Future.successful(FacebookProfileReader(json))
        }
    }
  }

}
