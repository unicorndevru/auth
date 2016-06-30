package auth.api

import auth.protocol.AuthStatus

trait AuthEvents {
  def userRegistered(status: AuthStatus)(implicit ctx: AuthRequestContext): Unit = ()
  def userAuthenticated(status: AuthStatus)(implicit ctx: AuthRequestContext): Unit = ()
}

object AuthEvents extends AuthEvents