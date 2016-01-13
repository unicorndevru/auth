package auth.providers.email

import auth.api._
import auth.providers.email.services.{ EmailChangeService, EmailVerifierService, PasswordChangeService, PasswordRecoveryService }
import auth.services.GravatarLinkService

import scala.concurrent.ExecutionContext

class EmailPasswordServices(
    authUsersService:     AuthUsersService,
    userIdentityService:  UserIdentitiesService,
    commandCryptoService: CredentialsCommandCrypto,

    passwordHasherService: PasswordHasherService,
    authMailsService:      AuthMailsService,

    gravatarLinkService: GravatarLinkService
)(implicit ec: ExecutionContext = ExecutionContext.global) {

  val emailChangeService = new EmailChangeService(
    authUsersService,
    userIdentityService,
    gravatarLinkService,
    passwordHasherService,
    commandCryptoService,
    authMailsService
  )

  val emailVerifierService = new EmailVerifierService(
    authMailsService,
    userIdentityService,
    commandCryptoService
  )

  val passwordChangeService = new PasswordChangeService(
    userIdentityService,
    passwordHasherService
  )

  val passwordRecoveryService = new PasswordRecoveryService(
    userIdentityService,
    authMailsService,
    passwordChangeService,
    commandCryptoService
  )
}
