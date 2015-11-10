package psgr.auth.actions

import psgr.auth.protocol.AuthUserId

/**
 * @author alari
 * @since 2/20/14
 */
trait UserRequired extends UserAware {

  override def userIdOpt: Option[AuthUserId] = Some(userId)

  def userId: AuthUserId
}
