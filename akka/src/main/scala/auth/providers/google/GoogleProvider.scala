package auth.providers.google

import auth.api.UserIdentitiesService
import auth.data.identity.{ IdentityId, OAuth2Info, UserIdentity }
import auth.providers.OAuth2Provider

import scala.concurrent.{ ExecutionContext, Future }
import ExecutionContext.Implicits.global

class GoogleProvider(
    googleApi:           GoogleApi,
    userIdentityService: UserIdentitiesService
) extends OAuth2Provider(userIdentityService) {
  val id = "google"

  def retrieveUserIdentity(info: OAuth2Info): Future[UserIdentity] = {
    googleApi.getMe(info).map {
      profile ⇒
        UserIdentity(
          identityId = IdentityId(providerId = id, userId = profile.userId.toString),
          firstName = profile.firstName,
          lastName = profile.lastName,
          fullName = profile.fullName,
          avatarUrl = profile.avatarURL,
          authMethod = info.method,
          oAuth2Info = Some(info),
          isEmailVerified = Some(profile.emailValue.isDefined),
          email = profile.emailValue
        )
    }
  }
}
