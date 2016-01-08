package auth.mongo

import auth.directives.AuthParams
import auth.testkit.AuthHandlerTestKit
import org.junit.runner.RunWith
import org.scalatest._
import reactivemongo.api.{ MongoConnection, MongoDriver, DefaultDB }

import scala.concurrent.Await
import scala.concurrent.duration._

@RunWith(classOf[junit.JUnitRunner])
class AuthMongoHandlerSpec extends AuthHandlerTestKit with BeforeAndAfterAll with MongoSupport {

  var db: DefaultDB = null

  override val handlerName: String = "mongo auth"

  lazy val composition = new MongoAuthServicesComposition(db) {
    override lazy val authParams: AuthParams = AuthParams("changeme")
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
