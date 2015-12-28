package auth.api

import akka.http.scaladsl.server.Directive1
import auth.protocol.AuthUserId

trait AuthDirectives {
  def userAware: Directive1[Option[AuthUserId]] = ???

  def userRequired: Directive1[AuthUserId] = ???
}
