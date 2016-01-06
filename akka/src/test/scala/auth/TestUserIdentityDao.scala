package auth

import java.util.UUID

import auth.core.UserIdentityDAO
import auth.data.identity.{ IdentityId, UserIdentity }
import auth.protocol.{ IdentitiesFilter, AuthError }

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TestUserIdentityDao extends UserIdentityDAO {

  var identities = TrieMap[String, UserIdentity]()

  override def get(id: IdentityId) = Future(identities.values.find(_.identityId == id).getOrElse(throw AuthError.IdentityNotFound))

  override def get(id: String) = Future.successful(identities(id))

  override def delete(id: IdentityId) = {
    identities = identities.filterNot{ case (k, v) ⇒ v.identityId == id }
    Future.successful(true)
  }

  override def upsert(u: UserIdentity) = {
    val id = u._id.getOrElse(UUID.randomUUID().toString)
    val uc = u.copy(_id = Some(id))
    identities += (id → uc)
    Future.successful(uc)
  }

  override def query(filter: IdentitiesFilter, offset: Int, limit: Int) = Future.successful(identities.values.filter {
    v ⇒
      filter.email.fold(true)(v.email.contains) && filter.profileId.fold(true)(v.profileId.contains)
  }.slice(offset, offset + limit).toList)
}