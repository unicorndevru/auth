package auth.providers.email

import auth.core.{ AuthMailsService, AuthUsersService, CredentialsCommandCrypto, UserIdentityService }
import auth.providers.email.services.{ EmailChangeService, EmailVerifierService, PasswordChangeService, PasswordRecoveryService }
import auth.services.GravatarLinkService

import scala.concurrent.ExecutionContext

class EmailPasswordServices(
    authUsersService:      AuthUsersService,
    userIdentityService:   UserIdentityService,
    passwordHasherService: PasswordHasherService,
    authMailsService:      AuthMailsService,

    commandCryptoService: CredentialsCommandCrypto,
    gravatarLinkService:  GravatarLinkService
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
