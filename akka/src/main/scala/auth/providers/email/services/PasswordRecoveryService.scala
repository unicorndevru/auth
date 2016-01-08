package auth.providers.email.services

import auth.api.{ AuthMailsService, CredentialsCommandCrypto, PasswordRecoverCommand, UserIdentitiesService }
import auth.data.identity.{ IdentityId, UserIdentity }
import auth.protocol.identities.UserIdentitiesFilter
import auth.protocol.{ AuthError, AuthUserId }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import io.circe.generic.auto._

class PasswordRecoveryService(
    userIdentityService:   UserIdentitiesService,
    authMailsService:      AuthMailsService,
    passwordChangeService: PasswordChangeService,
    commandCryptoService:  CredentialsCommandCrypto
) {

  /**
   * Will look for an identity with verified email and with password info (credentials identity)
   *
   * @param email email of the subject
   * @return nothing or error
   */
  def startRecovery(email: String): Future[Unit] =
    for {
      identity ← findIdentityByEmail(email)
      userId ← getUserIdFromIdentity(identity)
      secret = generateSecretLink(email, userId)
    } yield sendSecret(userId, secret)

  def finishRecovery(email: String, newPass: String): Future[AuthUserId] = {
    for {
      identity ← findIdentityByEmail(email)
      userId ← getUserIdFromIdentity(identity)
      _ ← recoverPassword(userId, newPass, email)
      _ ← sendNotification(identity, email)
    } yield userId
  }

  private def recoverPassword(userId: AuthUserId, newPass: String, email: String): Future[Unit] = {
    userIdentityService
      .queryAll(UserIdentitiesFilter(userId = Option(userId)))
      .map { list ⇒
        list.foreach { i ⇒ passwordChangeService.recoverPassword(identity = i, password = newPass) }
      }

    userIdentityService
      .queryAll(UserIdentitiesFilter(userId = Option(userId)))
      .map { list ⇒
        list.filter(_.email.contains(email)).foreach { i ⇒
          userIdentityService.updateExistingIdentity(i.copy(isEmailVerified = Some(true)))
        }
      }
  }

  private def sendNotification(identity: UserIdentity, email: String): Future[Unit] =
    for {
      list ← userIdentityService.queryAll(UserIdentitiesFilter(email = Some(email)))
      result ← sendForAll(list)
    } yield result

  private def sendForAll(users: List[UserIdentity]): Future[Unit] =
    Future.successful(users.flatMap(_.userId).foreach(authMailsService.passwordRecoverNotify))

  private def findIdentityByEmail(email: String): Future[UserIdentity] = {
    userIdentityService.find(IdentityId(providerId = "email", userId = email)).flatMap {
      case Some(identity) ⇒ Future.successful(identity)
      case _ ⇒ userIdentityService.queryAll(UserIdentitiesFilter(email = Some(email))).flatMap {
        list ⇒
          val filteredList = list.filter(i ⇒ i.passwordInfo.isDefined)
          if (filteredList.nonEmpty) Future.successful(filteredList.head)
          else Future.failed(AuthError.IdentityNotFound)
      }
    }
  }

  private def generateSecretLink(email: String, userId: AuthUserId): String =
    commandCryptoService.encrypt(PasswordRecoverCommand(email = email, id = userId))

  private def sendSecret(userId: AuthUserId, secret: String): Unit =
    authMailsService.passwordRecover(userId, secret)

  private def getUserIdFromIdentity(identity: UserIdentity): Future[AuthUserId] = identity.userId match {
    case Some(id) ⇒ Future.successful(id)
    case None     ⇒ Future.failed(AuthError.UserIdNotFound)
  }
}