package auth.providers.facebook

import auth.api.UserIdentitiesService
import auth.data.identity.{ IdentityId, OAuth2Info, UserIdentity }
import auth.providers.OAuth2Provider

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class FacebookProvider(facebookApi: FacebookApi, userIdentityService: UserIdentitiesService) extends OAuth2Provider(userIdentityService) {
  val id = "facebook"

  def retrieveUserIdentity(info: OAuth2Info): Future[UserIdentity] = {
    facebookApi.getMe(info).map {
      profile ⇒
        UserIdentity(
          locale = profile.locale,
          identityId = IdentityId(profile.userId, id),
          firstName = profile.firstName,
          lastName = profile.lastName,
          fullName = profile.name,
          avatarUrl = profile.avatarUrl,
          email = profile.email,
          isEmailVerified = Some(true),
          oAuth2Info = Some(info),
          authMethod = info.method,
          birthday = profile.birthday
        )
    }
  }
}
