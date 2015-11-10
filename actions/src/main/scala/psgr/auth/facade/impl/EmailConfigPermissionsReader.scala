package psgr.auth.facade.impl

import javax.inject.{ Inject, Singleton }

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader
import play.api.Configuration
import psgr.auth.facade.Permission
import psgr.auth.facade.Permission.All

case class SinglePermission(email: String, permissions: List[String])

case class PermissionsConfig(values: List[SinglePermission], adminsAll: Option[Boolean])

@Singleton
class EmailConfigPermissionsReader @Inject() (config: Configuration) {

  implicit val permissionsConfigReader: ValueReader[PermissionsConfig] = ValueReader.relative { permissionsConfig ⇒
    PermissionsConfig(
      values = permissionsConfig.as[List[SinglePermission]]("users"),
      adminsAll = permissionsConfig.as[Option[Boolean]]("all")
    )
  }

  implicit val singlePermissionConfigReader: ValueReader[SinglePermission] = ValueReader.relative { singlePermission ⇒
    SinglePermission(
      email = singlePermission.as[String]("email"),
      permissions = singlePermission.as[List[String]]("permissions")
    )
  }

  lazy val permissionsConfig = config.underlying.as[PermissionsConfig]("admins")

  def listPermissionsForEmail(email: Option[String]): Set[Permission] =
    if (permissionsConfig.adminsAll.contains(true)) {
      Set(All)
    } else {
      email.fold(Set.empty[Permission]) {
        e ⇒
          val assignedPermissionsAsList = permissionsConfig.values.filter(_.email == e).flatMap(perm ⇒ perm.permissions)
          assignedPermissionsAsList.flatMap(Permission.fromName).flatMap(_.flatten()).toSet
      }
    }

  def hasPermissionsForEmail(email: Option[String], permission: Permission*): Boolean = {
    val perms = listPermissionsForEmail(email)
    permission.forall(p ⇒ perms.exists(_.allows(p)))
  }
}

