package auth.services

import auth.api.{ AuthUsersService, UserIdentityService }
import auth.data.identity.{ IdentityId, UserIdentity }
import auth.protocol._
import auth.providers.Provider
import auth.providers.email.{ BCryptPasswordHasherService, EmailPasswordServices, PasswordHasherService }

import scala.concurrent.{ ExecutionContext, Future }

class AuthService(
    service:               AuthUsersService,
    emailPasswordServices: EmailPasswordServices,
    userIdentityService:   UserIdentityService,
    providers:             Set[Provider],
    pwdService:            PasswordHasherService = BCryptPasswordHasherService,
    gravatarLinkService:   GravatarLinkService   = GravatarLinkService
)(implicit ec: ExecutionContext = ExecutionContext.global) {

  private def authCommandToIdentityId(authObject: AuthorizeCommand): Future[IdentityId] = authObject match {
    case credentialsObject: AuthByCredentials ⇒ Future.successful(identityIdFromCredentials(credentialsObject))
    case tokenObject: AuthByToken             ⇒ Future.successful(identityIdFromToken(tokenObject))
    case _                                    ⇒ Future.failed(AuthError.WrongAuthObject)
  }

  import emailPasswordServices._

  def getRolePermissions(role: String): Future[Seq[String]] = service.getRolePermissions(role)

  def getStatus(id: AuthUserId): Future[AuthStatus] =
    service.getRoles(id).map(rs ⇒ AuthStatus(id, rs, None))

  def authorize(authObject: AuthorizeCommand): Future[Option[AuthStatus]] = {
    for {
      provider ← getProvider(authObject.provider)
      pid ← provider.authorize(authObject)
      s ← getStatus(pid)
    } yield Option(s)
  }

  def getProvider(id: String): Future[Provider] = providers.find(_.id == id) match {
    case Some(p) ⇒ Future.successful(p)
    case _       ⇒ Future.failed(AuthError.ProviderNotFound(id))
  }

  def register(authObject: AuthorizeCommand): Future[AuthStatus] =
    for {
      id ← authCommandToIdentityId(authObject)
      registered ← isRegistered(id)
      _ ← registeredPredicate(registered)
      userIdentity ← createIdentity(authObject)
      _ = emailVerifierService.sendVerifyingEmail(userIdentity.profileId.get)(userIdentity.email.get)
      Some(uid) = userIdentity.profileId
      s ← getStatus(uid)
    } yield s

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

    case _ ⇒ Future.failed(AuthError.WrongAuthObject)
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
    if (!registered) Future.successful(()) else Future.failed(AuthError.UserAlreadyRegistered)
}