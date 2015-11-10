package psgr.auth.actions

import play.api.mvc.{ RequestHeader, Result }
import psgr.auth.facade.AuthEnvironment
import psgr.auth.protocol.AuthUserId

import scala.concurrent.{ ExecutionContext, Future }

/**
 * @author alari
 * @since 2/20/14
 */
trait UserAware {
  def userIdOpt: Option[AuthUserId]
  def userIsOpt(opt: Option[AuthUserId]) = opt.exists(userIdOpt.contains)
  def userIs(id: AuthUserId) = userIdOpt.contains(id)
}

case class UserAwareWrapper(userIdOpt: Option[AuthUserId], wrapper: Result ⇒ Result) extends UserAware with (Result ⇒ Result) {
  def apply(r: Result): Result = wrapper(r)
}

object UserAwareWrapper {
  def wrap(rh: RequestHeader)(implicit ec: ExecutionContext, env: AuthEnvironment): UserAwareWrapper = {
    env.jwtAuthenticator.profileFromRequest(rh).fold {
      UserAwareWrapper(None, env.jwtAuthenticator.clean)
    } { pid ⇒
      UserAwareWrapper(Some(pid), env.jwtAuthenticator.refreshJwt(rh))
    }
  }

  def build(f: Option[AuthUserId] ⇒ Future[Result])(rh: RequestHeader)(implicit ec: ExecutionContext, env: AuthEnvironment): Future[Result] = {
    val w = wrap(rh)
    f(w.userIdOpt).map { resp ⇒
      resp.header.status match {
        case 401 ⇒
          env.jwtAuthenticator.clean(resp)
        case _ ⇒
          w(resp)
      }
    }
  }
}
