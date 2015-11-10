package psgr.auth.core.services

import javax.inject.Inject

import com.google.inject.ImplementedBy
import psgr.auth.UserIdentityModel
import psgr.auth.core._
import psgr.auth.facade.{ AuthUsersService, CreateUser }
import psgr.auth.protocol.{ IdentityFilter, AuthUserId }
import psgr.eventbus.LocalBus

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@ImplementedBy(classOf[UserIdentityServiceImpl])
trait UserIdentityService {
  def find(id: IdentityId): Future[Option[UserIdentityModel]]

  def get(id: IdentityId): Future[UserIdentityModel]

  def get(id: String): Future[UserIdentityModel]

  def createUser(identity: UserIdentityModel): Future[AuthUserId]

  def remove(identityId: IdentityId): Future[Boolean]

  def query(filter: IdentityFilter = IdentityFilter(None, None), offset: Int = 0, limit: Int = 0): Future[List[UserIdentityModel]]

  /**
   * Plain old save method. Doesn't create new user profile.
   *
   * @param identity - UserIdentityRecord
   * @return Future[UserIdentityRecord]
   */

  def saveNewIdentity(identity: UserIdentityModel): Future[UserIdentityModel]

  /**
   * Saves identity and creates new user profile for this identity
   * @param identity identity to create
   * @return saved identity with profileId
   */
  def saveNewIdentityAndCreateNewUser(identity: UserIdentityModel): Future[UserIdentityModel]

  /**
   * Updates existed identity. Doesn't create new user profile.
   * @param identity identity to update
   * @return updated identity
   */
  def updateExistingIdentity(identity: UserIdentityModel): Future[UserIdentityModel]
}

class UserIdentityServiceImpl @Inject() (dao: UserIdentityDAO, authUsersService: AuthUsersService, localBus: LocalBus) extends UserIdentityService {

  override def find(id: IdentityId): Future[Option[UserIdentityModel]] = get(id).map(Some(_)).recover({ case e ⇒ None })

  override def get(id: IdentityId): Future[UserIdentityModel] = dao.get(id).map { record ⇒ record: UserIdentityModel }

  override def get(id: String): Future[UserIdentityModel] = dao.get(id).map { record ⇒ record: UserIdentityModel }

  override def saveNewIdentityAndCreateNewUser(identity: UserIdentityModel): Future[UserIdentityModel] =
    find(identity.identityId)
      .flatMap {
        case Some(_) ⇒ Future.failed(new IllegalStateException("Cannot create identity. Cause identity already exist"))
        case None ⇒
          createUser(identity)
            .flatMap { uid ⇒
              dao.upsert(identity.copy(profileId = Some(uid)))
            }
      }
      .map{ i ⇒
        localBus.emitCreated(i)
      }

  override def saveNewIdentity(identity: UserIdentityModel): Future[UserIdentityModel] =
    dao
      .upsert(UserIdentityModel.modelToRecord(identity))
      .map { record ⇒
        localBus.emitCreated[UserIdentityModel](record)
      }

  override def updateExistingIdentity(identity: UserIdentityModel): Future[UserIdentityModel] = for {
    prev ← get(identity.identityId)
    curr ← dao.upsert(identity: UserIdentityRecord)
    _ = localBus.emitChanged(prev, curr: UserIdentityModel)
  } yield curr: UserIdentityModel

  override def createUser(user: UserIdentityModel): Future[AuthUserId] = authUsersService.create(CreateUser(
    firstName = user.firstName,
    lastName = user.lastName,
    fullName = user.fullName,
    avatarUrl = user.avatarUrl,
    facebookId = if (user.identityId.providerId == "facebook") Option(user.identityId.userId) else None,
    locale = user.locale,
    email = user.email
  ))

  override def remove(identityId: IdentityId): Future[Boolean] =
    dao.delete(identityId)

  override def query(filter: IdentityFilter = IdentityFilter(None, None), offset: Int = 0, limit: Int = 0): Future[List[UserIdentityModel]] =
    dao
      .query(filter, offset, limit)
      .map { list ⇒
        list
          .map { record ⇒
            record: UserIdentityModel
          }
      }
}