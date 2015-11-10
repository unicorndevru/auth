package psgr.auth.core.services

import java.util.NoSuchElementException
import javax.inject.Inject

import psgr.auth.protocol.{ IdentityFilter, AuthUserId }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class UserFinderService @Inject() (userIdentityService: UserIdentityService) {

  def getIdByEmail(email: String): Future[AuthUserId] = {
    userIdentityService
      .query(IdentityFilter(email = Some(email)))
      .flatMap { list ⇒
        list.headOption.flatMap { _.profileId } match {
          case Some(id) ⇒ Future.successful(id: AuthUserId)
          case None     ⇒ Future.failed(new NoSuchElementException(s"Cannot find user with this email = $email"))
        }
      }
  }
}
