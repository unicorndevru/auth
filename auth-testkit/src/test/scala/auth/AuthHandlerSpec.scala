package auth

import auth.api.{ AuthCryptoConfig, AuthUsersService, JwtCommandCrypto, UserIdentitiesDao }
import auth.directives.AuthParams
import auth.testkit.{ ActionSwitchTestKit, AuthHandlerTestKit, InMemoryAuthMailsProvider }
import org.junit.runner.RunWith
import org.scalatest._

@RunWith(classOf[junit.JUnitRunner])
class AuthHandlerSpec extends AuthHandlerTestKit with ActionSwitchTestKit {

  lazy val composition = new AuthServicesComposition with AuthCryptoConfig with InMemoryAuthMailsProvider {
    override lazy val authParams: AuthParams = AuthParams("changeme")
    override lazy val credentialsCommandCrypto = new JwtCommandCrypto(authParams.secretKey)

    override val userIdentityDao: UserIdentitiesDao = new TestUserIdentitiesDao
    override val authUserService: AuthUsersService = new TestAuthUsersService
  }

}
