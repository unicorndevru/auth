package psgr.auth.actions

import play.api.mvc.{ ActionBuilder, Request, Result }
import psgr.auth.facade.{ AuthEnvironment, Permission }

import scala.concurrent.Future

case class AuthorizedAction(perms: Permission*)(implicit env: AuthEnvironment) extends ActionBuilder[UserRequiredRequest] {
  implicit def ctx = executionContext

  def invokeBlock[A](request: Request[A], block: (UserRequiredRequest[A]) ⇒ Future[Result]): Future[Result] =
    UserAwareWrapper.build {
      case Some(pid) ⇒
        env.permissionsService.hasPermission(pid, perms: _*).flatMap {
          case true ⇒
            block(UserRequiredRequest(pid, request))
          case false ⇒
            Future successful env.forbiddenResponse(request)
        }
      case None ⇒
        Future successful env.unauthorizedResponse(request)
    }(request)

}