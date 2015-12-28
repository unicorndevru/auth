package auth.core

import auth.data.identity.UserIdentity

trait AuthEvents {
  def identityCreated(identity: UserIdentity): UserIdentity = identity
  def identityChanged(updated: UserIdentity, old: UserIdentity): UserIdentity = updated
}

object AuthEvents extends AuthEvents