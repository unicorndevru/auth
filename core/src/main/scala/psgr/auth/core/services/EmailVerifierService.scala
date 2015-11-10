package psgr.auth.core.services

import javax.inject.Inject

import play.api.Logger
import psgr.auth.UserIdentityModel
import psgr.auth.facade.AuthMailsService
import psgr.auth.protocol.{ IdentityFilter, AuthUserId }
import psgr.failures.JsonApiFailure

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailVerifierService @Inject() (
    authMailsService:     AuthMailsService,
    userIdentityService:  UserIdentityService,
    commandCryptoService: CommandCryptoService
) {

  private lazy val logger = Logger("email_verificator_service")

  /**
   * Procedure to start verifying all the user's emails
   * @param userId - id of the currerntly loged user
   * @return Unit or error.
   */
  def startVerify(userId: AuthUserId): Future[Unit] =
    userIdentityService
      .query(IdentityFilter(profileId = Option(userId)))
      .map { list ⇒
        if (list.isEmpty) {
          JsonApiFailure(400, "user_doesnt_have_emails", "Cannot find emails for user", "auth")
        } else {
          list
            .flatMap(_.email)
            .distinct
            .foreach(sendVerifyingEmail(userId))
        }
      }

  /**
   * Verify current email for all identities
   * @param email - email to verify
   * @return Unit or error
   */
  def verify(email: String): Future[Unit] =
    userIdentityService
      .query(IdentityFilter(email = Some(email)))
      .map { list ⇒
        list.foreach(setEmailVerified)
      }

  /**
   * Send mail to the email parameter with some secret for verifiyng (secret is encrypted email)
   * @param userId - id of the currently logged user
   * @param email - email to prove
   * @return - Unit
   */
  def sendVerifyingEmail(userId: AuthUserId)(email: String): Unit = {
    if (logger.isDebugEnabled) {
      logger.debug(s"Sending verifying email to the user with id = $userId, to the $email")
    }
    authMailsService
      .emailVerify(
        userId,
        email,
        commandCryptoService.encryptCommand(EmailVerifyCommand(email))
      )
  }

  /**
   * Updates identities record in db with email set verified
   * @param identity - UserRecordIdentity
   * @return - Future[UserRecordIdentity] or error
   */
  private def setEmailVerified(identity: UserIdentityModel): Future[UserIdentityModel] =
    userIdentityService
      .updateExistingIdentity(
        identity.copy(isEmailVerified = Some(true))
      )
}