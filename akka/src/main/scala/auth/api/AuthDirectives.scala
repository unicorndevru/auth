package auth.api

import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{ Directive0, Directive1 }
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

  def respondWithAuth: Directive0 = userAware.flatMap {
    case Some(a) ⇒ respondWithAuth(a.userId)
    case None    ⇒ pass
  }

  def respondWithAuth(id: AuthUserId): Directive0 =
    respondWithHeader(Authorization(OAuth2BearerToken(id.id)))

  def respondWithAuth(s: AuthStatus): Directive0 =
    respondWithAuth(s.userId)
}
