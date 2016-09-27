package auth.providers.vk

import auth.api.UserIdentitiesService
import auth.data.identity.{ IdentityId, OAuth2Info, UserIdentity }
import auth.providers.OAuth2Provider

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class VkProvider(
    vkApi:               VkApi,
    userIdentityService: UserIdentitiesService
) extends OAuth2Provider(userIdentityService) {
  val id = "vk"

  def retrieveUserIdentity(info: OAuth2Info): Future[UserIdentity] =
    vkApi.getMe(info).map {
      profile ⇒
        UserIdentity(
          identityId = IdentityId(providerId = id, userId = profile.userId.toString),
          firstName = profile.firstName,
          lastName = profile.lastName,
          fullName = for {
            fn ← profile.firstName
            ln ← profile.lastName
          } yield s"$fn $ln",
          avatarUrl = profile.avatarURL,
          authMethod = info.method,
          oAuth2Info = Some(info),
          isEmailVerified = Some(false),
          email = None
        )
    }
}
