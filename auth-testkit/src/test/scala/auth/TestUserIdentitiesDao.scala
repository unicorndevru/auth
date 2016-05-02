package auth

import java.util.UUID

import auth.api.UserIdentitiesDao
import auth.data.identity.{ IdentityId, UserIdentity }
import auth.protocol.AuthError
import auth.protocol.identities.UserIdentitiesFilter

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TestUserIdentitiesDao extends UserIdentitiesDao {

  var identities = TrieMap[String, UserIdentity]()

  override def get(id: IdentityId) = Future(identities.values.find(v ⇒ (v.identityId.userId equalsIgnoreCase id.userId) && (v.identityId.providerId equalsIgnoreCase id.providerId)).getOrElse(throw AuthError.IdentityNotFound))

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

  override def query(filter: UserIdentitiesFilter, offset: Int, limit: Int) =
    queryAll(filter).map(_.slice(offset, offset + limit))

  override def queryAll(filter: UserIdentitiesFilter) = Future.successful {
    identities.values.filter {
      v ⇒
        filter.email.fold(true)(v.email.contains) && filter.userId.fold(true)(v.userId.contains)
    }.toList
  }
}