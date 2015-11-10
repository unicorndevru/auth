package psgr.auth.actions

import play.api.mvc._
import psgr.auth.facade.AuthEnvironment
import psgr.auth.protocol.AuthUserId

import scala.concurrent.Future

class UserRequiredAction(implicit env: AuthEnvironment) extends ActionBuilder[UserRequiredRequest] {
  implicit def ctx = executionContext

  def invokeBlock[A](request: Request[A], block: (UserRequiredRequest[A]) ⇒ Future[Result]): Future[Result] =
    UserAwareWrapper.build {
      case Some(pid) ⇒
        block(UserRequiredRequest(pid, request))
      case None ⇒
        Future successful env.unauthorizedResponse(request)
    }(request)
}

object UserRequiredAction {
  implicit def convert(uaa: UserRequiredAction.type)(implicit env: AuthEnvironment): UserRequiredAction = new UserRequiredAction
}

case class UserRequiredRequest[A](userId: AuthUserId, request: Request[A]) extends WrappedRequest(request) with UserRequired