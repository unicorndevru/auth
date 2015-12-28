package auth.core

import auth.data.identity.{ IdentityId, UserIdentity }
import auth.protocol.IdentitiesFilter

import scala.concurrent.Future

trait UserIdentityDAO {
  def upsert(u: UserIdentity): Future[UserIdentity]

  def get(id: IdentityId): Future[UserIdentity]

  def get(id: String): Future[UserIdentity]

  def delete(u: UserIdentity): Future[Boolean] = delete(u.identityId)

  def delete(id: IdentityId): Future[Boolean]

  def query(filter: IdentitiesFilter, offset: Int, limit: Int): Future[List[UserIdentity]]
}
