package auth.providers

import auth.protocol.{ AuthUserId, AuthorizeCommand }
import play.api.libs.json.JsObject

import scala.concurrent.Future

trait Provider {
  val id: String

  def authorize(authObject: AuthorizeCommand, data: Option[JsObject]): Future[AuthUserId]
}
