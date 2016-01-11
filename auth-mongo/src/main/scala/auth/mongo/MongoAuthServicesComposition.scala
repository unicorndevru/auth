package auth.mongo

import auth.AuthServicesComposition
import auth.api.{ AuthUsersService, UserIdentitiesDao }
import auth.mongo.identities.MongoUserIdentitiesDao
import auth.mongo.users.SimpleMongoUserService
import reactivemongo.api.DefaultDB

abstract class MongoAuthServicesComposition(db: DefaultDB) extends AuthServicesComposition {
  override val authUserService: AuthUsersService = new SimpleMongoUserService(db)
  override val userIdentityDao: UserIdentitiesDao = new MongoUserIdentitiesDao(db)
}
