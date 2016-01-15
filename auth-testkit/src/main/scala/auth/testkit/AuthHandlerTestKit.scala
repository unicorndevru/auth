package auth.testkit

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{ RouteTestTimeout, ScalatestRouteTest }
import auth.AuthServicesComposition
import auth.handlers.{ AuthExceptionHandler, AuthHandler }
import auth.protocol._
import de.heikoseeberger.akkahttpcirce.CirceSupport
import org.scalatest._
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.time.{ Seconds, Span }

import scala.concurrent.duration.FiniteDuration

import io.circe._
import io.circe.generic.auto._

trait AuthHandlerTestKit extends WordSpec with ScalatestRouteTest with Matchers with ScalaFutures with TryValues
    with OptionValues with BeforeAndAfter with Eventually with CirceSupport {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))
  implicit val routeTimeout = RouteTestTimeout(FiniteDuration(5, TimeUnit.SECONDS))

  val composition: AuthServicesComposition with InMemoryAuthMailsProvider

  implicit val exceptionHandler = AuthExceptionHandler.generic

  lazy val route = Route.seal(new AuthHandler(composition).route)

  s"auth handler" should {
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

      eventually {
        InMemoryAuthMailsProvider.mailsAsTuple2() should contain(("emailVerify", st.userId))
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

      InMemoryAuthMailsProvider.mailsAsTuple2() should contain(("emailVerify", st.userId))

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
        InMemoryAuthMailsProvider.mailsAsTuple2() should contain(("passwordRecover", st.userId))
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

  override protected def beforeAll() = {
    InMemoryAuthMailsProvider.reset()
    super.beforeAll()
  }
}

