package auth

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{ RouteTestTimeout, ScalatestRouteTest }
import auth.api.{ AuthParams, AuthExceptionHandler }
import auth.core.DefaultUserIdentityService
import auth.handlers.AuthHandler
import auth.protocol._
import auth.providers.email.{ EmailCredentialsProvider, EmailPasswordServices }
import auth.services.AuthService
import de.heikoseeberger.akkahttpcirce.CirceSupport
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Seconds, Span }

import scala.concurrent.duration.FiniteDuration

import io.circe._
import io.circe.generic.auto._

@RunWith(classOf[junit.JUnitRunner])
class AuthHandlerSpec extends WordSpec with ScalatestRouteTest with Matchers with ScalaFutures with TryValues
    with OptionValues with BeforeAndAfter with CirceSupport {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))
  implicit val routeTimeout = RouteTestTimeout(FiniteDuration(5, TimeUnit.SECONDS))

  lazy val authUserService = new TestAuthUsersService

  lazy val userIdentityDao = new TestUserIdentityDao

  lazy val userIdentityService = new DefaultUserIdentityService(userIdentityDao, authUserService)

  lazy val emailPasswordServices = new EmailPasswordServices(authUserService, userIdentityService)

  lazy val service = new AuthService(authUserService, emailPasswordServices, userIdentityService, Set(new EmailCredentialsProvider(userIdentityService)))

  implicit val exceptionHandler = AuthExceptionHandler.generic

  lazy val route = Route.seal(new AuthHandler(service, emailPasswordServices, userIdentityService, AuthParams(secretKey = "changeme")).route)

  "auth handler" should {
    "return 401" in {
      Get("/auth") ~> route ~> check {
        status should be(StatusCodes.Unauthorized)
      }
    }

    "create a user" in {
      val cr = AuthByCredentials("email", "test@me.com", "123qwe")

      val Some(t) = Put("/auth", cr) ~> route ~> check {
        status should be(StatusCodes.Created)
        header("Authorization")
      }

      Put("/auth", cr) ~> route ~> check {
        status should be(StatusCodes.Conflict)
      }

      val st = Get("/auth").withHeaders(t) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[AuthStatus]
      }

      Post("/auth", cr) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[AuthStatus] should be(st)
        header("Authorization") should be('defined)
      }

      Post("/auth/actions/switch", SwitchUserCommand(AuthUserId("other"))).withHeaders(t) ~> route ~> check {
        status should be(StatusCodes.Forbidden)
      }
    }

    "switch user" in {

      val id = Put("/auth", AuthByCredentials("email", "test2@me.com", "123qwe")) ~> route ~> check {
        status should be(StatusCodes.Created)
        responseAs[AuthStatus].userId
      }

      val switchCr = AuthByCredentials("email", "switch,test@me.com", "123qwe")
      val Some(t) = Put("/auth", switchCr) ~> route ~> check {
        status should be(StatusCodes.Created)
        responseAs[AuthStatus].roles should contain("switch")
        header("Authorization")
      }

      val Some(st) = Post("/auth/actions/switch", SwitchUserCommand(id)).withHeaders(t) ~> route ~> check {
        status should be(StatusCodes.OK)
        val s = responseAs[AuthStatus]
        s.roles should not contain "switch"
        s.userId should be(id)
        s.originUserId should be('defined)
        header("Authorization")
      }

      Delete("/auth/actions/switch").withHeaders(st) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[AuthStatus].roles should contain("switch")
        responseAs[AuthStatus].originUserId should be('empty)
      }
    }

    "change password" in {
      val cr = AuthByCredentials("email", "test3@me.com", "123qwe")

      val Some(t) = Put("/auth", cr) ~> route ~> check {
        status should be(StatusCodes.Created)
        header("Authorization")
      }

      val st = Get("/auth").withHeaders(t) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[AuthStatus]
      }

      Post("/auth", cr) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[AuthStatus] should be(st)
        header("Authorization") should be('defined)
      }

      Post("/auth/actions/changePassword", PasswordChange(Some("123qwe"), "321ewq")).withHeaders(t) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[AuthStatus] should be(st)
      }

      Post("/auth", cr) ~> route ~> check {
        status should be(StatusCodes.Unauthorized)
        header("Authorization") should be('empty)
      }

      Post("/auth", cr.copy(password = "321ewq")) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[AuthStatus] should be(st)
        header("Authorization") should be('defined)
      }
    }
  }
}
