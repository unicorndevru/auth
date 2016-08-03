package auth.mongo

import auth.api.{ AuthCryptoConfig, JwtCommandCrypto }
import auth.directives.AuthParams
import auth.testkit.{ AuthHandlerTestKit, InMemoryAuthMailsProvider }
import org.scalatest._
import reactivemongo.api.{ DefaultDB, MongoConnection, MongoDriver }

import scala.concurrent.Await
import scala.concurrent.duration._

class AuthMongoHandlerSpec extends AuthHandlerTestKit with BeforeAndAfterAll with MongoSupport {

  var db: DefaultDB = null

  lazy val composition = new MongoAuthServicesComposition(db) with AuthCryptoConfig with InMemoryAuthMailsProvider {
    override lazy val authParams: AuthParams = AuthParams("changeme")

    override lazy val credentialsCommandCrypto = new JwtCommandCrypto(authParams.secretKey)
  }

  override protected def beforeAll() = {
    embeddedMongoStartup()
    val puri = MongoConnection.parseURI(mongouri).get
    db = Await.result(new MongoDriver().connection(puri).database(puri.db.get), 2 minutes)
    super.beforeAll()
  }

  override protected def afterAll() = {
    super.afterAll()
    embeddedMongoShutdown()
  }
}
