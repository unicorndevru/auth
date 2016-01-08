package auth

import auth.api.{ AuthUsersService, UserIdentitiesDao }
import auth.directives.AuthParams
import auth.testkit.{ ActionSwitchTestKit, AuthHandlerTestKit }
import org.junit.runner.RunWith
import org.scalatest._

@RunWith(classOf[junit.JUnitRunner])
class AuthHandlerSpec extends AuthHandlerTestKit with ActionSwitchTestKit {

  override val handlerName: String = "inmemory auth"

  lazy val composition = new AuthServicesComposition {
    override lazy val authParams: AuthParams = AuthParams("changeme")
    override val userIdentityDao: UserIdentitiesDao = new TestUserIdentitiesDao
    override val authUserService: AuthUsersService = new TestAuthUsersService
  }

}
