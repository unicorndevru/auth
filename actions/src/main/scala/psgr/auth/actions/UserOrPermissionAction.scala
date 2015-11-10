package psgr.auth.actions

import play.api.mvc.{ Request, Result }
import psgr.auth.facade.{ AuthEnvironment, Permission }
import psgr.failures.JsonApiFailure

import scala.concurrent.Future

case class UserOrPermissionAction(userId: String, permission: Permission)(implicit env: AuthEnvironment) extends UserRequiredAction {
  override def invokeBlock[A](request: Request[A], block: (UserRequiredRequest[A]) ⇒ Future[Result]): Future[Result] =
    super.invokeBlock(request, (r: UserRequiredRequest[A]) ⇒ (r.userId.id match {
      case `userId` ⇒
        Future.successful(true)
      case _ ⇒
        env.permissionsService.hasPermission(r.userId, permission)
    }).flatMap {
      case true ⇒
        block(r)
      case false ⇒
        Future.failed(JsonApiFailure(403, "forbidden", "Resource is inaccessible", "this"))
    })
}