package psgr.auth.core.providers

import psgr.auth.protocol.{ AuthUserId, AuthorizeCommand }
import psgr.failures.JsonApiFailure

import scala.concurrent.Future

/**
 * @author alari
 * @since 10/10/14
 */
trait Provider {
  def id: String

  def authorize(authObject: AuthorizeCommand): Future[AuthUserId]

  lazy val wrongAuthObjectError = JsonApiFailure(400, "wrong_auth_object", s"Wrong auth object. Cannot create access within $id provider.", "auth")

  lazy val failedPid = JsonApiFailure(400, "cannot_retrieve_profile_id", s"Cannot retrieve profile id within $id provider.", "auth")

  lazy val wrongAccess = new IllegalArgumentException(s"Wrong access object within $id provider.")
}