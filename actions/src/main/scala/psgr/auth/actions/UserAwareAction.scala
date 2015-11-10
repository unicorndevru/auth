package psgr.auth.actions

import play.api.mvc.{ ActionBuilder, Request, Result, WrappedRequest }
import psgr.auth.facade.AuthEnvironment
import psgr.auth.protocol.AuthUserId

import scala.concurrent.Future

/**
 * @author alari
 * @since 2/20/14
 */
class UserAwareAction(implicit env: AuthEnvironment) extends ActionBuilder[UserAwareRequest] {
  implicit def ctx = executionContext

  def invokeBlock[A](request: Request[A], block: (UserAwareRequest[A]) ⇒ Future[Result]): Future[Result] =
    UserAwareWrapper.build { profileIdOpt ⇒
      block(UserAwareRequest(profileIdOpt, request))
    }(request)
}

object UserAwareAction {
  implicit def convert(uaa: UserAwareAction.type)(implicit env: AuthEnvironment): UserAwareAction = new UserAwareAction
}

case class UserAwareRequest[A](userIdOpt: Option[AuthUserId], request: Request[A]) extends WrappedRequest(request) with UserAware