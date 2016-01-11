package auth

import auth.api._
import auth.providers.Provider
import auth.providers.email.{ BCryptPasswordHasherService, EmailCredentialsProvider, EmailPasswordServices, PasswordHasherService }
import auth.services.{ AuthService, GravatarLinkService }

trait AuthServicesComposition extends AuthCryptoConfig {

  val authUserService: AuthUsersService

  val userIdentityDao: UserIdentitiesDao

  lazy val userIdentityService: UserIdentitiesService = new DefaultUserIdentitiesService(userIdentityDao, authUserService)

  lazy val passwordHasherService: PasswordHasherService = BCryptPasswordHasherService
  lazy val authMailsService: AuthMailsService = LoggingAuthMailsService

  lazy val gravatarLinkService: GravatarLinkService = GravatarLinkService

  lazy val emailPasswordServices = new EmailPasswordServices(authUserService, userIdentityService, credentialsCommandCrypto, passwordHasherService, authMailsService, gravatarLinkService)

  lazy val credentialsProviders: Set[Provider] = Set(new EmailCredentialsProvider(userIdentityService))

  lazy val authService = new AuthService(authUserService, emailPasswordServices, userIdentityService, credentialsProviders, passwordHasherService, gravatarLinkService)

}
