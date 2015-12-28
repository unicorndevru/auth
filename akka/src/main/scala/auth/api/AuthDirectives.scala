package auth.api

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import auth.protocol.{ AuthStatus, AuthUserId }

trait AuthDirectives {
  private val authDir = authenticateOAuth2("auth", new Authenticator[AuthStatus] {
    override def apply(v1: Credentials) = v1 match {
      case Credentials.Provided(token) ⇒
        Some(
          AuthStatus(
            userId = AuthUserId(token),
            roles = Seq.empty,
            isSwitched = None
          )
        )
      case _ ⇒
        None
    }
  })

  def userAware: Directive1[Option[AuthStatus]] = authDir.optional

  def userRequired: Directive1[AuthStatus] = authDir
}
