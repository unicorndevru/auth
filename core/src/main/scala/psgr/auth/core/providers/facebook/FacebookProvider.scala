package psgr.auth.core.providers.facebook

import javax.inject.Inject

import psgr.auth.UserIdentityModel
import psgr.auth.core._
import psgr.auth.core.identity.{ OAuth2Info, IdentityId }
import psgr.auth.core.providers.OAuth2Provider
import psgr.auth.core.services.UserIdentityService

import scala.concurrent.{ ExecutionContext, Future }

class FacebookProvider @Inject() (facebookApi: FacebookApi, userIdentityService: UserIdentityService) extends OAuth2Provider(userIdentityService) {
  val id = "facebook"

  def retrieveUserIdentity(info: OAuth2Info)(implicit ec: ExecutionContext): Future[UserIdentityModel] = {
    facebookApi.getMe(info).map {
      profile ⇒
        UserIdentityModel(
          locale = profile.locale,
          identityId = IdentityId(profile.userId, id),
          firstName = profile.firstName,
          lastName = profile.lastName,
          fullName = profile.name,
          avatarUrl = profile.avatarUrl,
          email = profile.email,
          isEmailVerified = Some(true),
          oAuth2Info = Some(info),
          authMethod = info.method
        )
    }
  }
}