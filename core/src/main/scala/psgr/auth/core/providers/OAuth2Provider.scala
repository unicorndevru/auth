package psgr.auth.core.providers

import psgr.auth.UserIdentityModel
import psgr.auth.core.OAuth2Info
import psgr.auth.core.services.UserIdentityService
import psgr.auth.protocol.{ AuthByToken, AuthUserId, AuthorizeCommand }

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.implicitConversions

abstract class OAuth2Provider(userIdentityService: UserIdentityService) extends Provider {

  import scala.concurrent.ExecutionContext.Implicits.global

  def retrieveUserIdentity(info: OAuth2Info)(implicit ec: ExecutionContext): Future[UserIdentityModel]

  def authorize(authObject: AuthorizeCommand): Future[AuthUserId] = authObject match {
    case tokenObject: AuthByToken ⇒ for {
      identity ← retrieveUserIdentity(OAuth2Info(accessToken = tokenObject.token))
      userId ← getProfileId(identity)
    } yield userId
    case _ ⇒ Future.failed(wrongAuthObjectError)
  }

  protected def getProfileId(identity: UserIdentityModel): Future[AuthUserId] =
    storeIdentity(identity)
      .map {
        identity ⇒
          identity.profileId
      }.flatMap {
        case Some(userId) ⇒ Future.successful(userId)
        case _            ⇒ Future.failed(failedPid)
      }

  private def storeIdentity(newIdentity: UserIdentityModel): Future[UserIdentityModel] = {
    userIdentityService.find(newIdentity.identityId) flatMap {
      case Some(oldIdentity) ⇒ updateIdentity(oldIdentity, newIdentity)
      case None              ⇒ saveNewIdentity(newIdentity)
    }
  }

  private def updateIdentity(
    storedIdentity:    UserIdentityModel,
    retrievedIdentity: UserIdentityModel
  ): Future[UserIdentityModel] = {
    userIdentityService.updateExistingIdentity(storedIdentity.copy(
      locale = retrievedIdentity.locale,
      firstName = retrievedIdentity.firstName,
      lastName = retrievedIdentity.lastName,
      fullName = retrievedIdentity.fullName,
      avatarUrl = retrievedIdentity.avatarUrl,
      email = retrievedIdentity.email,
      isEmailVerified = retrievedIdentity.isEmailVerified,
      oAuth2Info = retrievedIdentity.oAuth2Info,
      authMethod = retrievedIdentity.authMethod
    ))
  }

  private def saveNewIdentity(user: UserIdentityModel): Future[UserIdentityModel] =
    userIdentityService.saveNewIdentityAndCreateNewUser(user)
}