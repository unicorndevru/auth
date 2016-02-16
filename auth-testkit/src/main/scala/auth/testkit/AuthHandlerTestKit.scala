package auth.testkit

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{ RouteTestTimeout, ScalatestRouteTest }
import auth.AuthServicesComposition
import auth.handlers.{ AuthJsonReads, AuthJsonWrites, AuthHandler }
import auth.protocol._
import utils.http.{ PlayJsonSupport, ApiErrorHandler }
import org.scalatest._
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.time.{ Seconds, Span }
import play.api.libs.json.Json

import scala.concurrent.duration.FiniteDuration

trait AuthHandlerTestKit extends WordSpec with ScalatestRouteTest with Matchers with ScalaFutures with TryValues
    with OptionValues with BeforeAndAfter with Eventually with PlayJsonSupport with AuthJsonWrites with AuthJsonReads {

  implicit val authByCredentialsWrites = Json.writes[AuthByCredentials]
  implicit val authStatusReads = Json.reads[AuthStatus]
  implicit val switchUserCommandWrites = Json.writes[SwitchUserCommand]
  implicit val passwordChangeWrites = Json.writes[PasswordChange]
  implicit val emailVerifyTokenWrites = Json.writes[EmailVerifyToken]
  implicit val startPasswordRecoverWrites = Json.writes[StartPasswordRecover]
  implicit val checkPasswordRecoverTokenWrites = Json.writes[CheckPasswordRecoverToken]
  implicit val finishPasswordRecoverWrites = Json.writes[FinishPasswordRecover]
  implicit val startEmailChangeWrites = Json.writes[StartEmailChange]
  implicit val finishEmailChangeWrites = Json.writes[FinishEmailChange]

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))
  implicit val routeTimeout = RouteTestTimeout(FiniteDuration(5, TimeUnit.SECONDS))

  val composition: AuthServicesComposition with InMemoryAuthMailsProvider

  implicit val exceptionHandler = ApiErrorHandler.generic

  lazy val route = Route.seal(new AuthHandler(composition).route)

  s"auth handler" should {
    "return 401" in {
      Get("/auth") ~> route ~> check {
        status should be(StatusCodes.Unauthorized)
        header("www-authenticate") should be('empty)
      }

      Post("/auth", AuthByCredentials("email", "testCheck@me.com", "123qwe")) ~> route ~> check {
        status should be(StatusCodes.NotFound)
      }
    }

    "create a user" in {
      val cr = AuthByCredentials("email", "testCreate@me.com", "123qwe")

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

      eventually {
        InMemoryAuthMailsProvider.contains("emailVerify", st.userId) should be(true)
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

    "send email validation requests" in {
      val cr = AuthByCredentials("email", "test22@me.com", "123qwe123")

      val Some(t) = Put("/auth", cr) ~> route ~> check {
        status should be(StatusCodes.Created)
        header("Authorization")
      }

      val st = Get("/auth").withHeaders(t) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[AuthStatus]
      }

      InMemoryAuthMailsProvider.contains("emailVerify", st.userId) should be(true)

      val letters = InMemoryAuthMailsProvider.getMailsByIdAndReason(st.userId, "emailVerify")

      letters should have size 1

      val payload = letters.head._3.asInstanceOf[(String, String)]

      payload._1 should equal ("test22@me.com")

      Post("/auth/actions/verifyEmail", EmailVerifyToken(payload._2)) ~> route ~> check {
        status should be(StatusCodes.NoContent)
      }

      val cr2 = AuthByCredentials("email", "test33@me.com", "123qwe123")

      val Some(t2) = Put("/auth", cr2) ~> route ~> check {
        status should be(StatusCodes.Created)
        header("Authorization")
      }

      val st2 = Get("/auth").withHeaders(t2) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[AuthStatus]
      }

      Post("/auth/actions/requestEmailVerify").withHeaders(t2) ~> route ~> check {
        status should be(StatusCodes.NoContent)
      }

      Post("/auth/actions/requestEmailVerify").withHeaders(t2) ~> route ~> check {
        status should be(StatusCodes.NoContent)
      }

      InMemoryAuthMailsProvider.getMailsById(st2.userId) should have size 3
    }

    "recover password" in {
      val cr = AuthByCredentials("email", "testRecoverPass@me.com", "123123")

      val Some(t) = Put("/auth", cr) ~> route ~> check {
        status should be(StatusCodes.Created)
        header("Authorization")
      }

      val st = Get("/auth").withHeaders(t) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[AuthStatus]
      }

      Post("/auth/actions/startPasswordRecovery", StartPasswordRecover("testRecoverPass@me.com")) ~> route ~> check {
        status should be(StatusCodes.NoContent)
      }

      eventually {
        InMemoryAuthMailsProvider.contains("passwordRecover", st.userId) should be(true)
      }

      val letters = InMemoryAuthMailsProvider.getMailsByIdAndReason(st.userId, "passwordRecover")

      letters should have size 1

      val token = letters.head._3.toString

      Post("/auth/actions/checkPasswordRecovery", CheckPasswordRecoverToken(token)) ~> route ~> check {
        status should be(StatusCodes.NoContent)
      }

      Post("/auth/actions/recoverPassword", FinishPasswordRecover(token, "321321")) ~> route ~> check {
        status should be(StatusCodes.OK)
      }

      eventually {
        Post("/auth", cr.copy(password = "321321")) ~> route ~> check {
          status should be(StatusCodes.OK)
          responseAs[AuthStatus] should be(st)
          header("Authorization") should be('defined)
        }
      }

    }

    "change email" in {
      val cr = AuthByCredentials("email", "testChangeEmail@me.com", "123123")

      val Some(t) = Put("/auth", cr) ~> route ~> check {
        status should be(StatusCodes.Created)
        header("Authorization")
      }

      val st = Get("/auth").withHeaders(t) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[AuthStatus]
      }

      Post("/auth/actions/startEmailChange", StartEmailChange("newEmail@me.com")).withHeaders(t) ~> route ~> check {
        status should be(StatusCodes.NoContent)
      }

      val letters = InMemoryAuthMailsProvider.getMailsByIdAndReason(st.userId, "changeEmail")

      letters should have size 1

      val payload = letters.head._3.asInstanceOf[(String, String)]

      payload._1 should equal ("newEmail@me.com")

      val st2 = Post("/auth/actions/finishEmailChange", FinishEmailChange(payload._2)) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[AuthStatus]
      }

      st.userId should equal (st2.userId)
    }
  }
}

