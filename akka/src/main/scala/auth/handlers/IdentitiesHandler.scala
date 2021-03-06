package auth.handlers

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import auth.api.UserIdentitiesService
import auth.data.identity.UserIdentity
import auth.directives.{ AuthDirectives, AuthParams }
import auth.protocol._
import auth.protocol.identities.{ AuthIdentitiesList, AuthIdentity, AuthIdentityId, UserIdentitiesFilter }

import scala.concurrent.{ ExecutionContext, Future }

class IdentitiesHandler(service: UserIdentitiesService, override val authParams: AuthParams)(implicit ec: ExecutionContext, mat: Materializer) extends AuthHandlerJson with AuthDirectives {

  def identityToProtocol(i: UserIdentity): AuthIdentity =
    AuthIdentity(
      id = i._id.get,
      identityId = AuthIdentityId(id = i.identityId.userId, provider = i.identityId.providerId),
      email = i.email,
      isEmailVerified = i.isEmailVerified
    )

  val route =
    (pathPrefix("identities") & userRequired) { status ⇒
      (get & pathEndOrSingleSlash) {
        val f = UserIdentitiesFilter(userId = Some(status.userId))
        complete(service.queryAll(f).map(is ⇒ AuthIdentitiesList(f, is.map(identityToProtocol))))
      } ~ (get & path(Segment)) { id ⇒
        complete(service.get(id).flatMap { ui ⇒
          if (ui.userId.contains(status.userId)) {
            Future.successful(identityToProtocol(ui))
          } else {
            Future.failed(AuthError.IdentityNotFound)
          }
        })
      }
    }

}
