package auth.api

import auth.data.identity.{ IdentityId, UserIdentity }
import auth.protocol.identities.UserIdentitiesFilter

import scala.concurrent.Future

trait UserIdentitiesDao {
  def upsert(u: UserIdentity): Future[UserIdentity]

  def get(id: IdentityId): Future[UserIdentity]

  def get(id: String): Future[UserIdentity]

  def delete(u: UserIdentity): Future[Boolean] = delete(u.identityId)

  def delete(id: IdentityId): Future[Boolean]

  def query(filter: UserIdentitiesFilter, offset: Int, limit: Int): Future[List[UserIdentity]]

  def queryAll(filter: UserIdentitiesFilter): Future[List[UserIdentity]]
}
