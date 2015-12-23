package psgr.auth.core.identity

import com.google.inject.ImplementedBy
import psgr.auth.protocol.IdentityFilter

import scala.concurrent.{ ExecutionContext, Future }

@ImplementedBy(classOf[MongoUserIdentityDao])
trait UserIdentityDAO {

  def upsert(u: UserIdentityRecord)(implicit ec: ExecutionContext): Future[UserIdentityRecord]

  def get(id: IdentityId)(implicit ec: ExecutionContext): Future[UserIdentityRecord]

  def get(id: String)(implicit ec: ExecutionContext): Future[UserIdentityRecord]

  def delete(u: UserIdentityRecord)(implicit ec: ExecutionContext): Future[Boolean] = delete(u.identityId)

  def delete(id: IdentityId)(implicit ec: ExecutionContext): Future[Boolean]

  def query(filter: IdentityFilter, offset: Int, limit: Int)(implicit ec: ExecutionContext): Future[List[UserIdentityRecord]]
}
