package auth.providers.email.services

import auth.api.{ AuthMailsService, CredentialsCommandCrypto, EmailVerifyCommand, UserIdentitiesService }
import auth.data.identity.UserIdentity
import auth.protocol.identities.UserIdentitiesFilter
import auth.protocol.{ AuthError, AuthUserId }

import scala.concurrent.{ ExecutionContext, Future }

class EmailVerifierService(
    authMailsService:     AuthMailsService,
    userIdentityService:  UserIdentitiesService,
    commandCryptoService: CredentialsCommandCrypto
)(implicit ec: ExecutionContext = ExecutionContext.global) {

  import auth.handlers.AuthJsonWrites.emailVerifyCommandWrites

  /**
   * Procedure to start verifying all the user's emails
   *
   * @param userId - id of the currerntly loged user
   * @return Unit or error.
   */
  def startVerify(userId: AuthUserId): Future[_] =
    userIdentityService
      .queryAll(UserIdentitiesFilter(userId = Option(userId)))
      .flatMap { list ⇒
        if (list.isEmpty) {
          Future.failed(AuthError.UserHaveNoEmails)
        } else {
          list.flatMap(_.email).distinct.foreach(sendVerifyingEmail(userId))
          Future.successful(())
        }
      }

  /**
   * Verify current email for all identities
   *
   * @param email - email to verify
   * @return Unit or error
   */
  def verify(email: String): Future[Unit] =
    userIdentityService
      .queryAll(UserIdentitiesFilter(email = Some(email)))
      .map { list ⇒
        list.foreach(setEmailVerified)
      }

  /**
   * Send mail to the email parameter with some secret for verifiyng (secret is encrypted email)
   *
   * @param userId - id of the currently logged user
   * @param email  - email to prove
   * @return - Unit
   */
  def sendVerifyingEmail(userId: AuthUserId)(email: String): Unit =
    authMailsService
      .emailVerify(
        userId,
        email,
        commandCryptoService.encrypt(EmailVerifyCommand(email))
      )

  /**
   * Updates identities record in db with email set verified
   *
   * @param identity - UserRecordIdentity
   * @return - Future[UserRecordIdentity] or error
   */
  private def setEmailVerified(identity: UserIdentity): Future[UserIdentity] =
    userIdentityService
      .updateExistingIdentity(
        identity.copy(isEmailVerified = Some(true))
      )
}