package auth.providers.email

import auth.core._
import auth.providers.email.services.{ EmailChangeService, EmailVerifierService, PasswordChangeService, PasswordRecoveryService }
import auth.services.GravatarLinkService

import scala.concurrent.ExecutionContext

class EmailPasswordServices(
    authUsersService:      AuthUsersService,
    userIdentityService:   UserIdentityService,
    passwordHasherService: PasswordHasherService = BCryptPasswordHasherService,
    authMailsService:      AuthMailsService      = LoggingAuthMailsService,

    commandCryptoService: CredentialsCommandCrypto = Base64UnsafeCommandCrypto,
    gravatarLinkService:  GravatarLinkService      = GravatarLinkService
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
