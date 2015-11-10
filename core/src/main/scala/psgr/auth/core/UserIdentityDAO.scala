package psgr.auth.core

import javax.inject.Inject

import com.google.inject.ImplementedBy
import play.modules.reactivemongo.ReactiveMongoApi
import psgr.auth.protocol.IdentityFilter
import reactivemongo.api.QueryOpts
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{ Index, IndexType }
import reactivemongo.bson.{ BSONDocument, BSONObjectID }
import reactivemongo.extensions.dao.BsonDao
import reactivemongo.extensions.dsl.BsonDsl._
import UserIdentityRecord._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

@ImplementedBy(classOf[UserIdentityJsonDao])
trait UserIdentityDAO {

  def upsert(u: UserIdentityRecord)(implicit ec: ExecutionContext): Future[UserIdentityRecord]

  def get(id: IdentityId)(implicit ec: ExecutionContext): Future[UserIdentityRecord]

  def get(id: String)(implicit ec: ExecutionContext): Future[UserIdentityRecord]

  def delete(u: UserIdentityRecord)(implicit ec: ExecutionContext): Future[Boolean] = delete(u.identityId)

  def delete(id: IdentityId)(implicit ec: ExecutionContext): Future[Boolean]

  def query(filter: IdentityFilter, offset: Int, limit: Int)(implicit ec: ExecutionContext): Future[List[UserIdentityRecord]]
}

class UserIdentityJsonDao @Inject() (mongoApi: ReactiveMongoApi) extends UserIdentityDAO {

  private object dao extends BsonDao[UserIdentityRecord, BSONObjectID](mongoApi.db, "user.identity") {
    override def autoIndexes = Seq(
      Index(Seq("identityId" → IndexType.Descending), unique = true, background = true, dropDups = true),
      Index(Seq("email" → IndexType.Descending, "identityId.providerId" → IndexType.Descending), background = true)
    )
  }

  override def upsert(u: UserIdentityRecord)(implicit ec: ExecutionContext): Future[UserIdentityRecord] = for {
    id ← getCorrectBsonId(u)
    record = u.copy(_id = Some(id))
    result ← dao.update("identityId" $eq record.identityId, record, upsert = true)
    if result.ok
  } yield record

  override def get(id: IdentityId)(implicit ec: ExecutionContext): Future[UserIdentityRecord] =
    dao.findOne("identityId" $eq id)
      .flatMap {
        case Some(userIdentityRecord) ⇒ Future.successful(userIdentityRecord)
        case None                     ⇒ Future.failed(new NoSuchElementException(s"Cannot find identity with id = $id"))
      }

  override def get(id: String)(implicit ec: ExecutionContext): Future[UserIdentityRecord] =
    (for {
      oId ← parseObjectId(id)
      user ← dao.findById(oId)
    } yield user)
      .flatMap {
        case Some(userIdentityRecord) ⇒ Future.successful(userIdentityRecord)
        case None                     ⇒ Future.failed(new NoSuchElementException(s"Cannot find identity with id = $id"))
      }

  override def delete(id: IdentityId)(implicit ec: ExecutionContext): Future[Boolean] =
    dao.remove("identityId" $eq id)
      .map(_.ok)

  override def query(filter: IdentityFilter, offset: Int, limit: Int)(implicit ec: ExecutionContext): Future[List[UserIdentityRecord]] =
    dao
      .collection
      .find(buildMatcher(filter))
      .options(QueryOpts(skipN = offset, batchSizeN = limit))
      .cursor[UserIdentityRecord]()
      .collect[List](if (limit > 0) limit else Int.MaxValue)

  private def buildMatcher(filter: IdentityFilter): BSONDocument = {
    val json = $doc()
    filter.profileId.fold(json)(id ⇒ "profileId" $eq id) ++
      filter.email.fold(json)(email ⇒ "email" $eq email)
  }

  private def failOrObj[T](obj: T)(err: WriteResult): Future[T] =
    if (err.inError) Future failed err else Future successful obj

  private def parseObjectId(id: String): Future[BSONObjectID] =
    BSONObjectID.parse(id).toOption match {
      case Some(parsedId) ⇒
        Future.successful(parsedId)
      case None ⇒
        Future.failed(new IllegalArgumentException(s"Cannot parse id = $id"))
    }

  private def getCorrectBsonId(u: UserIdentityRecord): Future[BSONObjectID] = {
    dao.findOne("identityId" $eq u.identityId).flatMap {
      case Some(record) ⇒
        record
          ._id
          .fold(Future.failed[BSONObjectID](new IllegalArgumentException("Id is not present."))) {
            Future.successful
          }
      case None ⇒
        u
          ._id
          .fold(Future.failed[BSONObjectID](new IllegalArgumentException("Id is not present."))) {
            Future.successful
          }

    }
  }
}