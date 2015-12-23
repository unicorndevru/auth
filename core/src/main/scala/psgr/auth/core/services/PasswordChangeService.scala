package psgr.auth.core.services

import javax.inject.Inject

import psgr.auth.UserIdentityModel
import psgr.auth.core.identity.{ AuthenticationMethod, PasswordInfo, IdentityId }
import psgr.auth.core.AuthenticationMethod
import psgr.auth.protocol.{ IdentityFilter, AuthUserId }
import psgr.failures.JsonApiFailure

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PasswordChangeService @Inject() (userIdentityService: UserIdentityService, passwordHasherService: PasswordHasherService) {

  /**
   * Changing password with all requirements. Checks old pass. Creates new credentials identity if user doesn't have credentials identity yet.
   *
   * @param userId user's id
   * @param oldPass old password (could be empty string, while creating credentials for social profile.)
   * @param newPass new password ()
   * @return created or edited credentials identity
   */
  def changePassword(userId: AuthUserId, oldPass: String, newPass: String): Future[List[UserIdentityModel]] = {
    userIdentityService
      .query(IdentityFilter(profileId = Option(userId)))
      .flatMap { list ⇒
        val idsWithPassword = list.filter(_.passwordInfo.isDefined)
        idsWithPassword
          .foldLeft(false)((b, identity) ⇒ b || checkPassword(identity, oldPass)) match {
            case true ⇒
              idsWithPassword.foreach(setNewPassword(_, newPass))
              Future.successful(idsWithPassword)
            case false ⇒
              Future.failed(JsonApiFailure(400, "wrong_password", "Wrong Password", "auth"))
          }
      }
  }

  /**
   * Recover password without checking old pass. If identity has password info, it will be overrided with new password info.
   *
   * @param identity identity where recover is happening
   * @param password password to set
   * @return edited identity
   */

  def recoverPassword(identity: UserIdentityModel, password: String): Future[UserIdentityModel] = identity.passwordInfo match {
    case Some(passwordInfo: PasswordInfo) ⇒ setNewPassword(identity, password)
    case _                                ⇒ Future.failed(JsonApiFailure(400, "identity_doesnot_have_password_info", "Identity is not suitable for recovery", "auth"))
  }

  private def checkPassword(identity: UserIdentityModel, password: String): Boolean = identity.passwordInfo match {
    case Some(passwordInfo: PasswordInfo) ⇒ passwordHasherService.validate(password, passwordInfo)
    case _                                ⇒ false
  }

  private def setNewPassword(identity: UserIdentityModel, password: String): Future[UserIdentityModel] =
    userIdentityService.updateExistingIdentity(identity.copy(passwordInfo = Some(passwordHasherService.createPasswordInfo(password))))

  private def createIdentityWithPassword(source: UserIdentityModel, newPass: String): Future[UserIdentityModel] = source.email match {
    case Some(email) if source.isEmailVerified.isDefined && source.isEmailVerified.get ⇒
      userIdentityService.updateExistingIdentity(copyIdentityAndSetPassword(source, newPass))
    case Some(email) ⇒
      Future.failed(JsonApiFailure(400, "cannot_change_password_email_isnt_verified", "Cannot change password, cause email isn't verefied", "auth"))
    case _ ⇒
      Future.failed(JsonApiFailure(400, "email_isnt_set", "Cannot set new pass, cause there is no email", "auth"))
  }

  private def copyIdentityAndSetPassword(identity: UserIdentityModel, newPass: String): UserIdentityModel = {
    identity.copy(
      identityId = IdentityId(providerId = "email", userId = identity.email.get),
      oAuth2Info = None,
      authMethod = AuthenticationMethod.UserPassword,
      passwordInfo = Some(passwordHasherService.createPasswordInfo(newPass))
    )
  }
}