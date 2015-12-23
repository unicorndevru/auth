package psgr.auth.core.services

import javax.inject.Inject

import play.api.Logger
import psgr.auth.UserIdentityModel
import psgr.auth.core.identity.IdentityId
import psgr.auth.facade.AuthMailsService
import psgr.auth.protocol.{ IdentityFilter, AuthUserId }
import psgr.failures.JsonApiFailure

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PasswordRecoveryService @Inject() (
    userIdentityService:   UserIdentityService,
    authMailsService:      AuthMailsService,
    passwordChangeService: PasswordChangeService,
    commandCryptoService:  CommandCryptoService
) {
  private val logger = Logger("password_recovery")

  /**
   * This method starts password recovery. It consumes only 1 argumet - email.
   * Will search suitable identity. Suitable is identity with verified email and with password info (credentials identity)
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
      .query(IdentityFilter(profileId = Option(userId)))
      .map { list ⇒
        list.foreach { i ⇒ passwordChangeService.recoverPassword(identity = i, password = newPass) }
      }

    userIdentityService
      .query(IdentityFilter(profileId = Option(userId)))
      .map { list ⇒
        list.filter(_.email.contains(email)).foreach { i ⇒
          userIdentityService.updateExistingIdentity(i.copy(isEmailVerified = Some(true)))
        }
      }
  }

  private def sendNotification(identity: UserIdentityModel, email: String): Future[Unit] =
    for {
      list ← userIdentityService.query(IdentityFilter(email = Some(email)))
      result ← sendForAll(list)
    } yield result

  private def sendForAll(users: List[UserIdentityModel]): Future[Unit] =
    Future.successful(users.flatMap(_.profileId).foreach(authMailsService.passwordRecoverNotify))

  private def findIdentityByEmail(email: String): Future[UserIdentityModel] = {
    userIdentityService.find(IdentityId(providerId = "email", userId = email)).flatMap {
      case Some(identity) ⇒ Future.successful(identity)
      case _ ⇒ userIdentityService.query(IdentityFilter(email = Some(email))).flatMap {
        list ⇒
          val filteredList = list.filter(i ⇒ i.passwordInfo.isDefined)
          if (filteredList.nonEmpty) Future.successful(filteredList.head)
          else Future.failed(JsonApiFailure(400, "cannot_find_identity_by_id", "Cannot find identity by id", "auth"))
      }
    }
  }

  private def generateSecretLink(email: String, userId: AuthUserId): String =
    commandCryptoService.encryptCommand(PasswordRecoverCommand(email = email, id = userId))

  private def sendSecret(userId: AuthUserId, secret: String): Unit =
    authMailsService.passwordRecover(userId, secret)

  private def getUserIdFromIdentity(identity: UserIdentityModel): Future[AuthUserId] = identity.profileId match {
    case Some(id) ⇒ Future.successful(id)
    case None     ⇒ Future.failed(JsonApiFailure(400, "identity_doesnt_have_profileId", "Cannot find ProfileId for this identity", "auth"))
  }
}