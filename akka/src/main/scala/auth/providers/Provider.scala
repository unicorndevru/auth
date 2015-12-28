package auth.providers

import auth.protocol.{ AuthUserId, AuthorizeCommand }

import scala.concurrent.Future

trait Provider {
  val id: String

  def authorize(authObject: AuthorizeCommand): Future[AuthUserId]
}
