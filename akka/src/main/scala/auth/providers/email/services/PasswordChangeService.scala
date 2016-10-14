package auth.providers.email.services

import auth.api.UserIdentitiesService
import auth.data.identity._
import auth.protocol.identities.UserIdentitiesFilter
import auth.protocol.{ AuthError, AuthUserId }
import auth.providers.email.PasswordHasherService

import scala.concurrent.{ ExecutionContext, Future }

class PasswordChangeService(userIdentityService: UserIdentitiesService, passwordHasherService: PasswordHasherService)(implicit ec: ExecutionContext = ExecutionContext.global) {

  /**
   * Changing password with all requirements. Checks old pass. Creates new credentials identity if user doesn't have credentials identity yet.
   *
   * @param userId user's id
   * @param oldPass old password (could be empty string, while creating credentials for social profile.)
   * @param newPass new password ()
   * @return created or edited credentials identity
   */
  def changePassword(userId: AuthUserId, oldPass: String, newPass: String): Future[List[UserIdentity]] = {
    userIdentityService
      .queryAll(UserIdentitiesFilter(userId = Option(userId)))
      .flatMap { list ⇒
        val idsWithPassword = list.filter(_.passwordInfo.isDefined)
        idsWithPassword
          .foldLeft(false)((b, identity) ⇒ b || checkPassword(identity, oldPass)) match {
            case true ⇒
              Future.traverse(idsWithPassword)(setNewPassword(_, newPass))
            case false if idsWithPassword.isEmpty && list.nonEmpty ⇒
              createIdentityWithPassword(list.head, newPass).map(_ :: list)
            case false ⇒
              Future.failed(AuthError.WrongPassword)
          }
      }
  }

  /**
   * Force password change without checks
   * @param userId user's id
   * @param newPass new password
   * @return updated identities
   */
  def setPassword(userId: AuthUserId, newPass: String): Future[List[UserIdentity]] = {
    userIdentityService
      .queryAll(UserIdentitiesFilter(userId = Option(userId)))
      .flatMap { list ⇒
        Future.traverse(list.filter(_.passwordInfo.isDefined))(setNewPassword(_, newPass))
      }
  }

  /**
   * Recover password without checking old pass. If identity has password info, it will be overriden with new password info.
   *
   * @param identity identity where recover is happening
   * @param password password to set
   * @return edited identity
   */
  def recoverPassword(identity: UserIdentity, password: String): Future[UserIdentity] = identity.passwordInfo match {
    case Some(passwordInfo: PasswordInfo) ⇒ setNewPassword(identity, password)
    case _                                ⇒ Future.failed(AuthError.UserHaveNoPassword)
  }

  private def checkPassword(identity: UserIdentity, password: String): Boolean = identity.passwordInfo match {
    case Some(passwordInfo: PasswordInfo) ⇒ passwordHasherService.validate(password, passwordInfo)
    case _                                ⇒ false
  }

  private def setNewPassword(identity: UserIdentity, password: String): Future[UserIdentity] =
    userIdentityService.updateExistingIdentity(identity.copy(passwordInfo = Some(passwordHasherService.createPasswordInfo(password))))

  private def createIdentityWithPassword(source: UserIdentity, newPass: String): Future[UserIdentity] = source.email match {
    case Some(email) if source.isEmailVerified.isDefined && source.isEmailVerified.get ⇒
      userIdentityService.updateExistingIdentity(copyIdentityAndSetPassword(source, newPass))
    case Some(email) ⇒
      Future.failed(AuthError.UserEmailNotVerified)
    case _ ⇒
      Future.failed(AuthError.UserHaveNoEmails)
  }

  private def copyIdentityAndSetPassword(identity: UserIdentity, newPass: String): UserIdentity = {
    identity.copy(
      identityId = IdentityId(providerId = "email", userId = identity.email.get),
      oAuth2Info = None,
      authMethod = AuthenticationMethod.UserPassword,
      passwordInfo = Some(passwordHasherService.createPasswordInfo(newPass))
    )
  }
}