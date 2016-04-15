package auth.mongo.identities

import auth.api.UserIdentitiesDao
import auth.data.identity.{ IdentityId, UserIdentity }
import auth.mongo.AuthDao
import auth.mongo.identities.UserIdentityRecord._
import auth.protocol.AuthError
import auth.protocol.identities.UserIdentitiesFilter
import reactivemongo.api.indexes.{ Index, IndexType }
import reactivemongo.api.{ DB, QueryOpts }
import reactivemongo.bson.{ BSONDocumentWriter, BSONObjectID }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MongoUserIdentitiesDao(db: DB) extends AuthDao[UserIdentityRecord, BSONObjectID](db, "user.identity") with UserIdentitiesDao {
  override def autoIndexes = Seq(
    Index(Seq("identityId" → IndexType.Descending), unique = true, background = true, dropDups = true),
    Index(Seq("email" → IndexType.Descending, "identityId.providerId" → IndexType.Descending), background = true)
  )

  private def recordToData(r: UserIdentityRecord): UserIdentity = UserIdentity(
    identityId = r.identityId,
    firstName = r.firstName,
    lastName = r.lastName,
    fullName = r.fullName,
    _id = Some(r._id.stringify),
    userId = r.userId,
    email = r.email,
    isEmailVerified = r.isEmailVerified,
    avatarUrl = r.avatarUrl,

    authMethod = r.authMethod,
    oAuth1Info = r.oAuth1Info,
    oAuth2Info = r.oAuth2Info,
    passwordInfo = r.passwordInfo,

    locale = r.locale
  )

  private def dataToRecord(r: UserIdentity): UserIdentityRecord = UserIdentityRecord(
    identityId = r.identityId,
    firstName = r.firstName,
    lastName = r.lastName,
    fullName = r.fullName,
    _id = r._id.fold(BSONObjectID.generate)(BSONObjectID.parse(_).get),
    userId = r.userId,
    email = r.email.map(_.toLowerCase),
    isEmailVerified = r.isEmailVerified,
    avatarUrl = r.avatarUrl,

    authMethod = r.authMethod,
    oAuth1Info = r.oAuth1Info,
    oAuth2Info = r.oAuth2Info,
    passwordInfo = r.passwordInfo,

    locale = r.locale
  )

  private implicit val filterWriter = new BSONDocumentWriter[UserIdentitiesFilter] {
    override def write(t: UserIdentitiesFilter) = {
      val d = $doc()
      t.userId.fold(d)(v ⇒ $doc("userId" → v.id)) ++
        t.email.fold(d)(v ⇒ $doc("email" → v.toLowerCase))
    }
  }

  override def upsert(u: UserIdentity) = {
    val r = dataToRecord(u)
    for {
      id ← getCorrectBsonId(r)
      record = r.copy(_id = id)
      result ← collection.update($doc("identityId" → record.identityId), record, upsert = true)
      if result.ok
    } yield recordToData(record)
  }

  override def get(id: IdentityId): Future[UserIdentity] =
    findOne($doc("identityId" → id))
      .flatMap {
        case Some(userIdentityRecord) ⇒ Future.successful(recordToData(userIdentityRecord))
        case None                     ⇒ Future.failed(AuthError.IdentityNotFound)
      }

  override def get(id: String): Future[UserIdentity] =
    (for {
      oId ← parseObjectId(id)
      user ← findById(oId)
    } yield user)
      .flatMap {
        case Some(userIdentityRecord) ⇒ Future.successful(recordToData(userIdentityRecord))
        case None                     ⇒ Future.failed(AuthError.IdentityNotFound)
      }

  override def delete(id: IdentityId): Future[Boolean] =
    collection.remove($doc("identityId" → id))
      .map(_.ok)

  override def query(filter: UserIdentitiesFilter, offset: Int, limit: Int): Future[List[UserIdentity]] =
    collection
      .find(filter)
      .options(QueryOpts(skipN = offset, batchSizeN = limit))
      .cursor[UserIdentityRecord]()
      .collect[List](limit).map(_.map(recordToData))

  override def queryAll(filter: UserIdentitiesFilter) =
    collection
      .find(filter)
      .cursor[UserIdentityRecord]()
      .collect[List]().map(_.map(recordToData))

  private def parseObjectId(id: String): Future[BSONObjectID] =
    BSONObjectID.parse(id).toOption match {
      case Some(parsedId) ⇒
        Future.successful(parsedId)
      case None ⇒
        Future.failed(new IllegalArgumentException(s"Cannot parse id = $id"))
    }

  private def getCorrectBsonId(u: UserIdentityRecord): Future[BSONObjectID] =
    findOne($doc("identityId" → u.identityId)).map(_.fold(u._id)(_._id))

}
