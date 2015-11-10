package psgr.auth.actions

import play.api.mvc.{ ActionBuilder, Request, Result, WrappedRequest }
import psgr.auth.facade.{ AuthEnvironment, Permission }
import psgr.auth.protocol.AuthUserId

import scala.concurrent.Future

case class UserResourceRequest[+A, T](userId: AuthUserId, resource: T, request: Request[A]) extends WrappedRequest(request) with UserRequired

class ResourceOrAdminAction[T](res: () ⇒ Future[T], lens: T ⇒ Option[AuthUserId])(implicit env: AuthEnvironment) extends ActionBuilder[({ type R[+A] = UserResourceRequest[A, T] })#R] {
  implicit def ctx = executionContext

  def invokeBlock[A](request: Request[A], block: (UserResourceRequest[A, T]) ⇒ Future[Result]): Future[Result] =
    UserAwareWrapper.build {
      case Some(pid) ⇒
        res().flatMap {
          r ⇒
            def req = UserResourceRequest(pid, r, request)

            lens(r) match {
              case Some(`pid`) ⇒
                block(req)
              case _ ⇒
                env.permissionsService.hasPermission(pid, Permission.Admin) flatMap {
                  case true ⇒
                    block(req)
                  case false ⇒
                    Future successful env.forbiddenResponse(request)
                }
            }
        }

      case None ⇒
        Future successful env.unauthorizedResponse(request)
    }(request)
}

object ResourceOrAdminAction {
  def apply[T](res: ⇒ Future[T])(lens: T ⇒ Option[AuthUserId])(implicit env: AuthEnvironment) = new ResourceOrAdminAction[T](() ⇒ res, lens)
}