package auth.providers

import auth.core.UserIdentityService
import auth.data.identity.{ OAuth2Info, UserIdentity }
import auth.protocol.{ AuthByToken, AuthError, AuthUserId, AuthorizeCommand }

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.implicitConversions

abstract class OAuth2Provider(service: UserIdentityService)(implicit ec: ExecutionContext = ExecutionContext.global) extends Provider {

  def retrieveUserIdentity(info: OAuth2Info): Future[UserIdentity]

  def authorize(authObject: AuthorizeCommand): Future[AuthUserId] = authObject match {
    case tokenObject: AuthByToken ⇒ for {
      identity ← retrieveUserIdentity(OAuth2Info(accessToken = tokenObject.token))
      userId ← getProfileId(identity)
    } yield userId
    case _ ⇒ Future.failed(AuthError.WrongAuthObject)
  }

  protected def getProfileId(identity: UserIdentity): Future[AuthUserId] =
    storeIdentity(identity).map(_.profileId).flatMap {
      case Some(userId) ⇒ Future.successful(userId)
      case _            ⇒ Future.failed(AuthError.UserIdNotFound)
    }

  private def storeIdentity(newIdentity: UserIdentity): Future[UserIdentity] =
    service.find(newIdentity.identityId).flatMap {
      case Some(oldIdentity) ⇒ updateIdentity(oldIdentity, newIdentity)
      case None              ⇒ saveNewIdentity(newIdentity)
    }

  private def updateIdentity(
    storedIdentity:    UserIdentity,
    retrievedIdentity: UserIdentity
  ): Future[UserIdentity] = {
    import retrievedIdentity._
    service.updateExistingIdentity(storedIdentity.copy(
      locale = locale,
      firstName = firstName,
      lastName = lastName,
      fullName = fullName,
      avatarUrl = avatarUrl,
      email = email,
      isEmailVerified = isEmailVerified,
      oAuth2Info = oAuth2Info,
      authMethod = authMethod
    ))
  }

  private def saveNewIdentity(user: UserIdentity): Future[UserIdentity] =
    service.saveNewIdentityAndCreateNewUser(user)
}