package auth.mongo

import auth.AuthServicesComposition
import auth.api.{ AuthUsersService, UserIdentitiesDao }
import auth.mongo.identities.MongoUserIdentitiesDao
import auth.mongo.users.SimpleMongoUserService
import reactivemongo.api.DB

abstract class MongoAuthServicesComposition(db: DB) extends AuthServicesComposition {
  override val authUserService: AuthUsersService = new SimpleMongoUserService(db)
  override val userIdentityDao: UserIdentitiesDao = new MongoUserIdentitiesDao(db)
}
