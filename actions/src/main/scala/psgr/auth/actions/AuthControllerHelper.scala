package psgr.auth.actions

import javax.inject.Inject

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import psgr.auth.facade.{ AuthEnvironment, Permission }
import psgr.auth.protocol.AuthUserId
import psgr.failures.JsonApiFailure

import scala.concurrent.Future

trait AuthControllerHelper {
  self: Controller ⇒
  @Inject private var env: AuthEnvironment = null

  implicit def authEnv: AuthEnvironment = {
    env
  }

  def adminOrUser(is: AuthUserId)(mustBe: Option[AuthUserId]): Future[Unit] = mustBe match {
    case Some(`is`) ⇒
      Future.successful(())
    case _ ⇒
      authEnv.permissionsService.hasPermission(is, Permission.Admin).flatMap {
        case true  ⇒ Future.successful(())
        case false ⇒ Future.failed(JsonApiFailure(403, "forbidden", "Forbidden", "this"))
      }
  }
}
