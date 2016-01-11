package auth

import auth.api.{ JwtCommandCrypto, AuthCryptoConfig, AuthUsersService, UserIdentitiesDao }
import auth.directives.AuthParams
import auth.testkit.{ ActionSwitchTestKit, AuthHandlerTestKit }
import org.junit.runner.RunWith
import org.scalatest._

@RunWith(classOf[junit.JUnitRunner])
class AuthHandlerSpec extends AuthHandlerTestKit with ActionSwitchTestKit {

  lazy val composition = new AuthServicesComposition with AuthCryptoConfig {
    override lazy val authParams: AuthParams = AuthParams("changeme")
    override lazy val credentialsCommandCrypto = new JwtCommandCrypto(authParams.secretKey)

    override val userIdentityDao: UserIdentitiesDao = new TestUserIdentitiesDao
    override val authUserService: AuthUsersService = new TestAuthUsersService
  }

}
