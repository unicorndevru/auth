package psgr.auth.facade.impl

import javax.inject.Inject

import psgr.auth.facade.Permission.{ Carry, SwitchUser, Admin, All }
import psgr.auth.facade.{ AuthPermissionsService, AuthUsersService, Permission }
import psgr.auth.protocol.AuthUserId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailConfigPermissionsService @Inject() (reader: EmailConfigPermissionsReader, authUsersService: AuthUsersService) extends AuthPermissionsService {
  override def permissions(id: AuthUserId): Future[Set[Permission]] =
    if (reader.permissionsConfig.adminsAll.contains(true)) {
      Future.successful(All.flatten().toSet)
    } else {
      authUsersService.findEmail(id).map(reader.listPermissionsForEmail).recover {
        case _: NoSuchElementException ⇒
          Set.empty
      }
    }
}
