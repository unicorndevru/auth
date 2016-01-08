package auth

import auth.api._
import auth.directives.AuthParams
import auth.providers.Provider
import auth.providers.email.{ BCryptPasswordHasherService, EmailCredentialsProvider, EmailPasswordServices, PasswordHasherService }
import auth.services.{ AuthService, GravatarLinkService }
import com.typesafe.config.ConfigFactory

import scala.util.Try

trait AuthServicesComposition {
  val authUserService: AuthUsersService

  val userIdentityDao: UserIdentitiesDao

  private lazy val config = ConfigFactory.load()

  lazy val authParams = AuthParams(
    secretKey = config.getString("auth.secretKey"),
    expireIn = Try(config.getInt("auth.expireIn")).getOrElse(86400),
    issuer = Try(config.getString("auth.issuer")).toOption,
    audience = Try(config.getString("auth.audience")).toOption
  )

  lazy val credentialsCommandCrypto: CredentialsCommandCrypto =
    new JwtCommandCrypto(Try(config.getString("auth.cryptoKey")).getOrElse(authParams.secretKey))

  lazy val userIdentityService: UserIdentitiesService = new DefaultUserIdentitiesService(userIdentityDao, authUserService)

  lazy val passwordHasherService: PasswordHasherService = BCryptPasswordHasherService
  lazy val authMailsService: AuthMailsService = LoggingAuthMailsService

  lazy val gravatarLinkService: GravatarLinkService = GravatarLinkService

  lazy val emailPasswordServices = new EmailPasswordServices(authUserService, userIdentityService, credentialsCommandCrypto, passwordHasherService, authMailsService, gravatarLinkService)

  lazy val credentialsProviders: Set[Provider] = Set(new EmailCredentialsProvider(userIdentityService))

  lazy val authService = new AuthService(authUserService, emailPasswordServices, userIdentityService, credentialsProviders, passwordHasherService, gravatarLinkService)

}
