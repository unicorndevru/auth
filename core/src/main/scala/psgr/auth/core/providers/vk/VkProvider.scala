package psgr.auth.core.providers.vk

import javax.inject.Inject

import psgr.auth.UserIdentityModel
import psgr.auth.core.providers.OAuth2Provider
import psgr.auth.core.services.UserIdentityService
import psgr.auth.core.{ IdentityId, OAuth2Info }

import scala.concurrent.{ ExecutionContext, Future }

class VkProvider @Inject() (
    vkApi:               VkApi,
    userIdentityService: UserIdentityService
) extends OAuth2Provider(userIdentityService) {
  val id = "vk"

  def retrieveUserIdentity(info: OAuth2Info)(implicit ec: ExecutionContext): Future[UserIdentityModel] =
    vkApi.getMe(info).map {
      profile ⇒
        UserIdentityModel(
          identityId = IdentityId(providerId = id, userId = profile.userId.toString),
          firstName = profile.firstName.getOrElse(""),
          lastName = profile.lastName.getOrElse(""),
          fullName = s"${profile.firstName.getOrElse("")} ${profile.lastName.getOrElse("")} ",
          avatarUrl = profile.avatarURL,
          authMethod = info.method,
          oAuth2Info = Some(info),
          isEmailVerified = Some(false),
          email = None
        )
    }
}