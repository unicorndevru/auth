package auth.testkit

import akka.http.scaladsl.model.StatusCodes
import auth.protocol.{ SwitchUserCommand, AuthStatus, AuthByCredentials }

import io.circe._
import io.circe.generic.auto._

trait ActionSwitchTestKit {
  self: AuthHandlerTestKit ⇒
  s"auth action switch handler" should {
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
  }
}
