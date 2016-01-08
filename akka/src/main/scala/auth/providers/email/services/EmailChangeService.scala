package auth.providers.email.services

import auth.api._
import auth.data.identity._
import auth.protocol.AuthUserId
import auth.protocol.identities.UserIdentitiesFilter
import auth.providers.email.PasswordHasherService
import auth.services.GravatarLinkService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

import io.circe.generic.auto._

class EmailChangeService(
    authUsersService:      AuthUsersService,
    userIdentityService:   UserIdentitiesService,
    gravatarLinkService:   GravatarLinkService,
    passwordHasherService: PasswordHasherService,
    commandCryptoService:  CredentialsCommandCrypto,
    authMailsService:      AuthMailsService
) {

  def start(userId: AuthUserId, newEmail: String): Unit = sendHash(newEmail, userId, generateHash(newEmail, userId))

  def finish(userId: AuthUserId, newEmail: String): Future[AuthUserId] = {
    val randomString = Random.alphanumeric.take(8).mkString
    val newAvatarUrl = gravatarLinkService.generateLink(newEmail).toOption
    val identity = UserIdentity(
      firstName = None,
      lastName = None,
      fullName = None,
      identityId = IdentityId(providerId = "email", userId = newEmail),
      email = Some(newEmail),
      isEmailVerified = Some(true),
      authMethod = AuthenticationMethod.UserPassword,
      passwordInfo = Some(passwordHasherService.createPasswordInfo(randomString)),
      userId = Some(userId),
      avatarUrl = newAvatarUrl
    )
    findOldPassword(userId).flatMap {
      case Some(pwdInfo) ⇒
        saveNewEmailIdentity(identity.copy(passwordInfo = Some(pwdInfo)))
      case _ ⇒
        sendNewPassword(userId, randomString)
        saveNewEmailIdentity(identity)
    }
  }

  /**
   * Finds if user already has credentials identity and sets password from that identity to new email identity.
   *
   * @param userId - UserId
   * @return - Option wrpapped into Future
   */

  private def findOldPassword(userId: AuthUserId): Future[Option[PasswordInfo]] = {
    userIdentityService
      .queryAll(UserIdentitiesFilter(userId = Option(userId)))
      .map { list ⇒
        list
          .filter(_.passwordInfo.isDefined)
          .flatMap(_.passwordInfo)
          .headOption
      }
  }

  private def sendNewPassword(userId: AuthUserId, newPassword: String): Unit =
    authMailsService.newPassword(userId, newPassword)

  private def saveNewEmailIdentity(identity: UserIdentity): Future[AuthUserId] =
    for {
      identity ← userIdentityService.saveNewIdentity(identity)
      userId ← authUsersService.setEmail(identity.userId.get, identity.email.get, identity.avatarUrl)
    } yield userId

  private def generateHash(newEmail: String, userId: AuthUserId): String = {
    commandCryptoService.encrypt(ChangeEmailCommand(newEmail = newEmail, userId = userId))
  }

  private def sendHash(newEmail: String, userId: AuthUserId, hash: String): Unit =
    authMailsService.changeEmail(userId, newEmail, hash)
}