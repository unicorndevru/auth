package psgr.auth.facade

import psgr.auth.protocol.AuthUserId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AuthPermissionsService {
  def permissions(id: AuthUserId): Future[Set[Permission]]

  def hasPermission(id: AuthUserId, perms: Permission*): Future[Boolean] =
    permissions(id).map(_.intersect(perms.toSet).size == perms.size)

  def hasAdminPermission(id: AuthUserId): Future[Boolean] =
    hasPermission(id, Permission.Admin)
}
