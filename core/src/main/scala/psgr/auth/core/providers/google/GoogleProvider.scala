package psgr.auth.core.providers.google

import javax.inject.Inject

import psgr.auth.UserIdentityModel
import psgr.auth.core.providers.OAuth2Provider
import psgr.auth.core.services.UserIdentityService
import psgr.auth.core.{ IdentityId, OAuth2Info }

import scala.concurrent.{ ExecutionContext, Future }

class GoogleProvider @Inject() (
    googleApi:           GoogleApi,
    userIdentityService: UserIdentityService
) extends OAuth2Provider(userIdentityService) {
  val id = "google"

  def retrieveUserIdentity(info: OAuth2Info)(implicit ec: ExecutionContext): Future[UserIdentityModel] = {
    googleApi.getMe(info).map {
      profile ⇒
        UserIdentityModel(
          identityId = IdentityId(providerId = id, userId = profile.userId.toString),
          firstName = profile.firstName.getOrElse(""),
          lastName = profile.lastName.getOrElse(""),
          fullName = profile.fullName.getOrElse(""),
          avatarUrl = profile.avatarURL,
          authMethod = info.method,
          oAuth2Info = Some(info),
          isEmailVerified = Some(profile.emailValue.isDefined),
          email = profile.emailValue
        )
    }
  }
}