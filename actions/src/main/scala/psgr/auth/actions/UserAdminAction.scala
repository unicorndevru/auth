package psgr.auth.actions

import psgr.auth.facade.{ AuthEnvironment, Permission }

class UserAdminAction(implicit env: AuthEnvironment) extends AuthorizedAction(Permission.Admin)

object UserAdminAction {
  implicit def convert(uaa: UserAdminAction.type)(implicit env: AuthEnvironment): UserAdminAction = new UserAdminAction
}
