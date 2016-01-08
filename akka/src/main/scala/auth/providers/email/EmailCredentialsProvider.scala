package auth.providers.email

import auth.api.UserIdentityService
import auth.data.identity.{ IdentityId, UserIdentity }
import auth.protocol._
import auth.providers.Provider

import scala.concurrent.{ ExecutionContext, Future }

class EmailCredentialsProvider(service: UserIdentityService, pwdService: PasswordHasherService = BCryptPasswordHasherService)(implicit ec: ExecutionContext = ExecutionContext.global) extends Provider {

  override val id = "email"

  override def authorize(authObject: AuthorizeCommand): Future[AuthUserId] = authObject match {
    case credentials: AuthByCredentials ⇒
      for {
        identity ← service.get(IdentityId(userId = credentials.email, providerId = credentials.provider))
        _ ← checkIdentityAndPassword(credentials.password, identity)
        userId ← extractUserId(identity)
      } yield userId
    case _ ⇒
      Future.failed(AuthError.WrongAuthObject)
  }

  private def checkIdentityAndPassword(password: String, identity: UserIdentity): Future[Boolean] =
    identity.passwordInfo match {
      case Some(passwordInfo) ⇒
        if (pwdService.validate(password, passwordInfo)) Future.successful(true)
        else Future.failed(AuthError.WrongPassword)
      case None ⇒
        Future.failed(AuthError.WrongPassword)
    }

  private def extractUserId(identity: UserIdentity): Future[AuthUserId] = identity.profileId match {
    case Some(userId) ⇒ Future.successful(userId)
    case None         ⇒ Future.failed(AuthError.UserIdNotFound)
  }

}
