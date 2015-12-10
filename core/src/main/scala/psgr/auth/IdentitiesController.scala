package psgr.auth

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc.Controller
import psgr.auth.actions.{ AuthControllerHelper, ResourceOrAdminAction, UserRequiredAction }
import psgr.auth.core.services.UserIdentityService
import psgr.auth.protocol.{ AuthIdentitiesList, AuthIdentity, IdentityFilter }

import scala.concurrent.ExecutionContext.Implicits.global

class IdentitiesController @Inject() (userIdentityService: UserIdentityService) extends Controller with AuthControllerHelper {

  def get(id: String) =
    ResourceOrAdminAction(userIdentityService.get(id))(_.profileId).apply {
      implicit request ⇒
        Ok(Json.toJson(request.resource: AuthIdentity))
    }

  def getAll = UserRequiredAction.async {
    implicit request ⇒
      for {
        list ← userIdentityService.query(IdentityFilter(profileId = request.userIdOpt))
      } yield Ok(Json.toJson(AuthIdentitiesList(items = list.map(l ⇒ l: AuthIdentity))))
  }
}
