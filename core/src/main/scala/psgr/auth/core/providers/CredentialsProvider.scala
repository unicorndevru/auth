package psgr.auth.core.providers

import javax.inject.Inject

import psgr.auth.UserIdentityModel
import psgr.auth.core.IdentityId
import psgr.auth.core.services.{ PasswordHasherService, UserIdentityService }
import psgr.auth.protocol.{ AuthByCredentials, AuthUserId, AuthorizeCommand }
import psgr.failures.JsonApiFailure

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

class CredentialsProvider @Inject() (
    userIdentityService: UserIdentityService,
    pwdService:          PasswordHasherService
) extends Provider {

  implicit def credentialsToIdentityId(credentials: AuthByCredentials): IdentityId =
    IdentityId(providerId = credentials.provider, userId = credentials.email)

  lazy val wrongPassword = JsonApiFailure(400, "wrong_password", "Wrong Password.", "auth")
  lazy val wrongLogin = JsonApiFailure(400, "wrong_login", "Wrong Login", "auth")
  lazy val wrongUserId = JsonApiFailure(400, "cannot_find_user_id", "Corrupted identity.", "auth")

  override def id: String = "email"

  override def authorize(authObject: AuthorizeCommand): Future[AuthUserId] = authObject match {
    case credentials: AuthByCredentials ⇒
      for {
        identity ← userIdentityService.get(credentials)
        _ ← checkIdentityAndPassword(credentials.password, identity)
        userId ← extractUserId(identity)
      } yield userId
    case _ ⇒
      Future.failed(wrongAuthObjectError)
  }

  private def checkIdentityAndPassword(password: String, identity: UserIdentityModel): Future[Boolean] =
    identity.passwordInfo match {
      case Some(passwordInfo) ⇒
        if (pwdService.validate(password, passwordInfo)) Future.successful(true)
        else Future.failed(wrongPassword)
      case None ⇒
        Future.failed(wrongPassword)
    }

  private def extractUserId(identity: UserIdentityModel): Future[AuthUserId] = identity.profileId match {
    case Some(userId) ⇒ Future.successful(userId)
    case None         ⇒ Future.failed(wrongUserId)
  }
}