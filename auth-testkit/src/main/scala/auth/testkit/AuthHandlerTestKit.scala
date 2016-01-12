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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Seconds, Span }

import scala.concurrent.duration.FiniteDuration

import io.circe._
import io.circe.generic.auto._

trait AuthHandlerTestKit extends WordSpec with ScalatestRouteTest with Matchers with ScalaFutures with TryValues
    with OptionValues with BeforeAndAfter with CirceSupport {

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

      InMemoryAuthMailsProvider.mails.map(k ⇒ (k._1, k._2)) should contain(("emailVerify", st.userId))

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

    }
  }
}

