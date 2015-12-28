package auth.services

import auth.core.UserIdentityService
import auth.data.identity.{ IdentityId, UserIdentity }
import auth.protocol._
import auth.providers.email.PasswordHasherService
import auth.providers.email.services.EmailVerifierService

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.implicitConversions

class ProfileRegistrarService(
    userIdentityService:  UserIdentityService,
    emailVerifierService: EmailVerifierService,
    pwdService:           PasswordHasherService,
    gravatarLinkService:  GravatarLinkService
)(implicit ec: ExecutionContext = ExecutionContext.global) {

  private def authCommandToIdentityId(authObject: AuthorizeCommand): Future[IdentityId] = authObject match {
    case credentialsObject: AuthByCredentials ⇒ Future.successful(identityIdFromCredentials(credentialsObject))
    case tokenObject: AuthByToken             ⇒ Future.successful(identityIdFromToken(tokenObject))
    case _                                    ⇒ Future.failed(AuthError.WrongAuthObject)
  }

  def register(authObject: AuthorizeCommand): Future[AuthUserId] =
    for {
      id ← authCommandToIdentityId(authObject)
      registered ← isRegistered(id)
      _ ← registeredPredicate(registered)
      userIdentity ← createIdentity(authObject)
      _ = emailVerifierService.sendVerifyingEmail(userIdentity.profileId.get)(userIdentity.email.get)
      Some(uid) = userIdentity.profileId
    } yield uid

  def isRegistered(identityId: IdentityId): Future[Boolean] = {
    userIdentityService.find(identityId).flatMap {
      case Some(identity) ⇒ Future.successful(true)
      case _              ⇒ Future.successful(false)
    }
  }

  def createIdentity(authObject: AuthorizeCommand): Future[UserIdentity] = authObject match {
    case credentialsObject: AuthByCredentials ⇒
      val passwordInfo = pwdService.createPasswordInfo(credentialsObject.password)
      val defaultName = credentialsObject.email.split("@").headOption
      val gravatarUrl = gravatarLinkService.generateLink(credentialsObject.email)
      val userIdentity =
        UserIdentity(
          identityId = identityIdFromCredentials(credentialsObject),
          firstName = defaultName,
          lastName = None,
          fullName = defaultName,
          avatarUrl = gravatarUrl.toOption,
          email = Some(credentialsObject.email),
          isEmailVerified = Some(false),
          authMethod = passwordInfo.method,
          passwordInfo = Some(passwordInfo)
        )
      userIdentityService.saveNewIdentityAndCreateNewUser(userIdentity)

    case _ ⇒ Future.failed(
      new IllegalArgumentException(s"Cannot create identity with profile registrar for provider : ${authObject.provider}, this way isn't supported yet")
    )
  }

  def isEmailRegistered(email: String): Future[Boolean] =
    userIdentityService
      .get(IdentityId(providerId = "email", userId = email))
      .map(_ ⇒ true)
      .recover({ case e ⇒ false })

  private def identityIdFromCredentials(credentials: AuthByCredentials): IdentityId =
    IdentityId(providerId = credentials.provider, userId = credentials.email)

  private def identityIdFromToken(token: AuthByToken): IdentityId =
    IdentityId(providerId = token.provider, userId = token.token)

  private def registeredPredicate(registered: Boolean) =
    if (!registered) Future.successful(()) else Future.failed(new IllegalArgumentException("User already registered!"))
}