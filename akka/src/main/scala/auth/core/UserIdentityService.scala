package auth.core

import auth.data.identity.{ IdentityId, UserIdentity }
import auth.protocol.{ IdentitiesFilter, AuthError, AuthUserId }

import scala.concurrent.{ ExecutionContext, Future }

trait UserIdentityService {
  def find(id: IdentityId): Future[Option[UserIdentity]]

  def get(id: IdentityId): Future[UserIdentity]

  def get(id: String): Future[UserIdentity]

  def createUser(identity: UserIdentity): Future[AuthUserId]

  def remove(identityId: IdentityId): Future[Boolean]

  def query(filter: IdentitiesFilter = IdentitiesFilter(None, None), offset: Int = 0, limit: Int = 0): Future[List[UserIdentity]]

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
  def saveNewIdentityAndCreateNewUser(identity: UserIdentity): Future[UserIdentity]

  /**
   * Updates existed identity. Doesn't create new user profile.
   *
   * @param identity identity to update
   * @return updated identity
   */
  def updateExistingIdentity(identity: UserIdentity): Future[UserIdentity]
}

class DefaultUserIdentityService(dao: UserIdentityDAO, service: AuthUsersService, events: AuthEvents = AuthEvents)(implicit ec: ExecutionContext = ExecutionContext.global) extends UserIdentityService {

  override def find(id: IdentityId): Future[Option[UserIdentity]] = get(id).map(Some(_)).recover({ case e ⇒ None })

  override def get(id: IdentityId): Future[UserIdentity] = dao.get(id)

  override def get(id: String): Future[UserIdentity] = dao.get(id)

  override def saveNewIdentityAndCreateNewUser(identity: UserIdentity): Future[UserIdentity] =
    find(identity.identityId)
      .flatMap {
        case Some(_) ⇒ Future.failed(AuthError.DuplicateIdentities)
        case None ⇒
          createUser(identity)
            .flatMap { uid ⇒
              dao.upsert(identity.copy(profileId = Some(uid)))
            }
      }
      .map(events.identityCreated)

  override def saveNewIdentity(identity: UserIdentity): Future[UserIdentity] =
    dao.upsert(identity).map(events.identityCreated)

  override def updateExistingIdentity(identity: UserIdentity): Future[UserIdentity] = for {
    prev ← get(identity.identityId)
    curr ← dao.upsert(identity)
    _ = events.identityChanged(curr, prev)
  } yield curr

  override def createUser(user: UserIdentity): Future[AuthUserId] = service.create(CreateUser(
    firstName = user.firstName,
    lastName = user.lastName,
    fullName = user.fullName,
    avatarUrl = user.avatarUrl,
    locale = user.locale,
    email = user.email
  ))

  override def remove(identityId: IdentityId): Future[Boolean] =
    dao.delete(identityId)

  override def query(filter: IdentitiesFilter = IdentitiesFilter(), offset: Int = 0, limit: Int = 0): Future[List[UserIdentity]] =
    dao.query(filter, offset, limit)

}