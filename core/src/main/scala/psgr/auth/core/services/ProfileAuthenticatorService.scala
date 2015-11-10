package psgr.auth.core.services

import com.google.inject.Inject
import psgr.auth.core.providers.{ AuthProviders, Provider }
import psgr.auth.protocol.{ AuthUserId, AuthorizeCommand }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
/**
 * @author alari
 * @since 2/20/14
 */
class ProfileAuthenticatorService @Inject() (userIdentityService: UserIdentityService, providersRegistry: AuthProviders) {

  lazy private val providers = providersRegistry.providers

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