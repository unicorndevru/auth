package auth

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{ RouteTestTimeout, ScalatestRouteTest }
import auth.api.AuthExceptionHandler
import auth.core.{ UserIdentityDAO, CreateUser, AuthUsersService, DefaultUserIdentityService }
import auth.data.identity.{ IdentityId, UserIdentity }
import auth.protocol._
import auth.providers.email.{ EmailCredentialsProvider, EmailPasswordServices }
import auth.services.AuthService
import de.heikoseeberger.akkahttpcirce.CirceSupport
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest._
import org.scalatest.time.{ Seconds, Span }

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

import io.circe._
import io.circe.generic.auto._

@RunWith(classOf[junit.JUnitRunner])
class AuthHandlerSpec extends WordSpec with ScalatestRouteTest with Matchers with ScalaFutures with TryValues
    with OptionValues with BeforeAndAfter with CirceSupport {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))
  implicit val routeTimeout = RouteTestTimeout(FiniteDuration(5, TimeUnit.SECONDS))

  lazy val authUserService = new AuthUsersService {
    var users = TrieMap[AuthUserId, CreateUser]()

    var id = 0

    override def setEmail(id: AuthUserId, email: String, avatar: Option[String]) = {
      val u = users(id)
      users(id) = users(id).copy(email = Some(email))
      Future.successful(id)
    }

    override def findEmail(id: AuthUserId) = Future.successful(users.get(id).flatMap(_.email))

    override def create(cmd: CreateUser) = {
      id += 1
      val aid = AuthUserId("user-" + id.toString)
      users += (aid → cmd)
      Future.successful(aid)
    }
  }

  lazy val userIdentityDao = new UserIdentityDAO {

    var identities = TrieMap[String, UserIdentity]()

    override def get(id: IdentityId) = Future(identities.values.find(_.identityId == id).getOrElse(throw AuthError.IdentityNotFound))

    override def get(id: String) = Future.successful(identities(id))

    override def delete(id: IdentityId) = {
      identities = identities.filterNot{ case (k, v) ⇒ v.identityId == id }
      Future.successful(true)
    }

    override def upsert(u: UserIdentity) = {
      val id = u._id.getOrElse(UUID.randomUUID().toString)
      val uc = u.copy(_id = Some(id))
      identities += (id → uc)
      Future.successful(uc)
    }

    override def query(filter: IdentitiesFilter, offset: Int, limit: Int) = Future.successful(identities.values.filter {
      v ⇒
        filter.email.fold(true)(v.email.contains) && filter.profileId.fold(true)(v.profileId.contains)
    }.slice(offset, offset + limit).toList)
  }

  lazy val userIdentityService = new DefaultUserIdentityService(userIdentityDao, authUserService)

  lazy val emailPasswordServices = new EmailPasswordServices(authUserService, userIdentityService)

  lazy val service = new AuthService(emailPasswordServices, userIdentityService, Set(new EmailCredentialsProvider(userIdentityService)))

  implicit val exceptionHandler = AuthExceptionHandler.generic

  lazy val route = Route.seal(new AuthHandler(service).route)

  "auth handler" should {
    "return 401" in {
      Get("/auth") ~> route ~> check {
        status should be(StatusCodes.Unauthorized)
      }
    }

    "create a user" in {
      val cr = AuthByCredentials("email", "test@me.com", "123qwe")

      val Some(t) = Put("/auth", cr) ~> route ~> check {
        status should be(StatusCodes.Created)
        header("Authorization")
      }

      val st = Get("/auth").withHeaders(t) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[AuthStatus]
      }

      Post("/auth", cr) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[AuthStatus] should be(st)
      }
    }
  }
}
