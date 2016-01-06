package auth

import java.util.UUID

import auth.core.UserIdentityDAO
import auth.data.identity.{ IdentityId, UserIdentity }
import auth.protocol.{ AuthError, IdentitiesFilter }

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TestUserIdentityDao extends UserIdentityDAO {

  var identities = TrieMap[String, UserIdentity]()

  override def get(id: IdentityId) = Future(identities.values.find(_.identityId == id).getOrElse(throw AuthError.IdentityNotFound))

  override def get(id: String) = Future.successful(identities(id))

  override def delete(id: IdentityId) = {
    identities = identities.filterNot { case (k, v) ⇒ v.identityId == id }
    Future.successful(true)
  }

  override def upsert(u: UserIdentity) = {
    val id = u._id.getOrElse(UUID.randomUUID().toString)
    val uc = u.copy(_id = Some(id))
    identities += (id → uc)
    Future.successful(uc)
  }

  override def query(filter: IdentitiesFilter, offset: Int, limit: Int) = Future.successful {
    val is = identities.values.filter {
      v ⇒
        filter.email.fold(true)(v.email.contains) && filter.profileId.fold(true)(v.profileId.contains)
    }.toList

    if (limit > 0) is.slice(offset, offset + limit) else is

  }
}