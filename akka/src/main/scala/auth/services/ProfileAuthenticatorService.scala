package auth.services

import auth.core.UserIdentityService
import auth.protocol.{ AuthUserId, AuthorizeCommand }
import auth.providers.Provider

import scala.concurrent.{ ExecutionContext, Future }

/**
 * @author alari
 * @since 2/20/14
 */
class ProfileAuthenticatorService(userIdentityService: UserIdentityService, providers: Set[Provider])(implicit ec: ExecutionContext = ExecutionContext.global) {

  def authorize(authObject: AuthorizeCommand): Future[Option[AuthUserId]] = {
    for {
      provider ← getProvider(authObject.provider)
      pid ← provider.authorize(authObject)
    } yield Option(pid)
  }

  def getProvider(id: String): Future[Provider] = providers.find(p ⇒ p.id == id) match {
    case Some(p) ⇒ Future.successful(p)
    case _       ⇒ Future.failed(new NoSuchElementException(s"Cannot find provider with id = $id"))
  }
}