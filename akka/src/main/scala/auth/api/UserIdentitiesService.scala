package auth.api

import auth.data.identity.{ IdentityId, UserIdentity }
import auth.protocol.identities.UserIdentitiesFilter
import auth.protocol.{ AuthError, AuthUserId }
import play.api.libs.json.JsObject

import scala.concurrent.{ ExecutionContext, Future }

trait UserIdentitiesService {
  def find(id: IdentityId): Future[Option[UserIdentity]]

  def get(id: IdentityId): Future[UserIdentity]

  def get(id: String): Future[UserIdentity]

  def createUser(identity: UserIdentity, data: Option[JsObject]): Future[AuthUserId]

  def remove(identityId: IdentityId): Future[Boolean]

  def query(filter: UserIdentitiesFilter = UserIdentitiesFilter(None, None), offset: Int, limit: Int): Future[List[UserIdentity]]

  def queryAll(filter: UserIdentitiesFilter = UserIdentitiesFilter(None, None)): Future[List[UserIdentity]]

  /**
   * Plain old save method. Doesn't create new user profile.
   *
   * @param identity - UserIdentityRecord
   * @return Future[UserIdentityRecord]
   */

  def saveNewIdentity(identity: UserIdentity): Future[UserIdentity]

  /**
   * Saves identity and creates new user profile for this identity
   *
   * @param identity identity to create
   * @return saved identity with profileId
   */
  def saveNewIdentityAndCreateNewUser(identity: UserIdentity, data: Option[JsObject] = None): Future[UserIdentity]

  /**
   * Updates existed identity. Doesn't create new user profile.
   *
   * @param identity identity to update
   * @return updated identity
   */
  def updateExistingIdentity(identity: UserIdentity): Future[UserIdentity]
}

class DefaultUserIdentitiesService(dao: UserIdentitiesDao, service: AuthUsersService)(implicit ec: ExecutionContext = ExecutionContext.global) extends UserIdentitiesService {

  override def find(id: IdentityId): Future[Option[UserIdentity]] = get(id).map(Some(_)).recover({ case e ⇒ None })

  override def get(id: IdentityId): Future[UserIdentity] = dao.get(id)

  override def get(id: String): Future[UserIdentity] = dao.get(id)

  override def saveNewIdentityAndCreateNewUser(identity: UserIdentity, data: Option[JsObject] = None): Future[UserIdentity] =
    find(identity.identityId)
      .flatMap {
        case Some(_) ⇒ Future.failed(AuthError.DuplicateIdentities)
        case None ⇒
          createUser(identity, data)
            .flatMap { uid ⇒
              dao.upsert(identity.copy(userId = Some(uid)))
            }
      }

  override def saveNewIdentity(identity: UserIdentity): Future[UserIdentity] =
    dao.upsert(identity)

  override def updateExistingIdentity(identity: UserIdentity): Future[UserIdentity] = for {
    prev ← get(identity.identityId)
    curr ← dao.upsert(identity)
  } yield curr

  override def createUser(user: UserIdentity, data: Option[JsObject]): Future[AuthUserId] = service.create(CreateUser(
    firstName = user.firstName,
    lastName = user.lastName,
    fullName = user.fullName,
    avatarUrl = user.avatarUrl,
    locale = user.locale,
    data = data,
    email = user.email
  ))

  override def remove(identityId: IdentityId): Future[Boolean] =
    dao.delete(identityId)

  override def query(filter: UserIdentitiesFilter = UserIdentitiesFilter(), offset: Int, limit: Int): Future[List[UserIdentity]] =
    dao.query(filter, offset, limit)

  override def queryAll(filter: UserIdentitiesFilter) =
    dao.queryAll(filter)
}