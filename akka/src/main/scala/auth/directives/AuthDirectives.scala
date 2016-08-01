package auth.directives

import java.time.Instant

import akka.http.scaladsl.model.RemoteAddress
import akka.http.scaladsl.model.headers.{ CustomHeader, `User-Agent` }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ AuthorizationFailedRejection, Directive0, Directive1 }
import auth.api.AuthRequestContext
import auth.protocol.{ AuthError, AuthStatus, AuthUserId }
import auth.services.AuthService
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{ JwtAlgorithm, JwtClaim, JwtJson }
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

case class AuthParams(
  secretKey: String,
  expireIn:  Int              = 86400,
  issuer:    Option[String]   = Some("auth"),
  audience:  Set[String]      = Set.empty,
  algo:      JwtHmacAlgorithm = JwtAlgorithm.HS256
)

case class AuthClaimData(
  r: Set[String],
  o: Option[String]
)

case class SetAuthorization(token: String) extends CustomHeader {
  override def value() = s"Bearer $token"

  override def name() = "Authorization"

  override def renderInResponses() = true

  override def renderInRequests() = true
}

object AuthClaimData {
  implicit val fmt = Json.format[AuthClaimData]
}

trait AuthDirectives {
  val authParams: AuthParams

  import authParams._

  def extractAuthRequestContext: Directive1[AuthRequestContext] =
    ((extractClientIP.map(addr ⇒ Some(addr)) | provide(Option.empty[RemoteAddress])) & optionalHeaderValueByType[`User-Agent`]() & userAware).tflatMap{
      case (addr, ua, s) ⇒ provide(new AuthRequestContext {
        override def remoteAddress = addr

        override def userAgent = ua.map(_.value())

        override def status = s
      })
    }

  def userAware(implicit reqCtx: AuthRequestContext = null): Directive1[Option[AuthStatus]] = Option(reqCtx) match {
    case Some(ctx) ⇒
      provide(ctx.status)
    case None ⇒
      optionalHeaderValueByName("Authorization")
        .map(_.filter(_.startsWith("Bearer ")).map(_.stripPrefix("Bearer ")))
        .map {

          case Some(token) ⇒

            JwtJson.decode(token, secretKey, Seq(JwtAlgorithm.HS256))
              .filter(claim ⇒
                issuer.fold(claim.isValid)(iss ⇒
                  ((audience.nonEmpty && audience.exists(claim.isValid(iss, _))) || claim.isValid(iss))
                    && claim.subject.isDefined))
              .toOption
              .map { claim ⇒
                val s = AuthStatus(
                  userId = AuthUserId(claim.subject.get),
                  roles = Set.empty,
                  originUserId = None
                )

                Try(Json.parse(claim.content).validate[AuthClaimData]).toOption.flatMap(_.asOpt).fold(s)(
                  d ⇒ s.copy(
                    roles = d.r,
                    originUserId = d.o.map(AuthUserId)
                  )
                )
              }

          case _ ⇒
            None
        }
  }

  val userStringIdAware: Directive1[Option[String]] = userAware.map(_.map(_.userId.id))

  def userRequired(implicit reqCtx: AuthRequestContext = null): Directive1[AuthStatus] = userAware.flatMap {
    case Some(a) ⇒ provide(a)
    case None    ⇒ failWith(AuthError.Unauthorized)
  }

  val userStringIdRequired: Directive1[String] = userRequired.map(_.userId.id)

  def rolesRequired(s: AuthStatus, rs: String*): Directive1[AuthStatus] =
    if (rs.forall(rs.contains)) {
      provide(s)
    } else {
      reject(AuthorizationFailedRejection)
    }

  def rolesRequired(rs: String*)(implicit reqCtx: AuthRequestContext = null): Directive1[AuthStatus] = userRequired.flatMap { s ⇒
    rolesRequired(s, rs: _*)
  }

  def respondWithAuth(implicit reqCtx: AuthRequestContext = null): Directive0 = userAware.flatMap {
    case Some(a) ⇒ respondWithAuth(a)
    case None    ⇒ pass
  }

  def respondWithAuth(s: AuthStatus): Directive0 = {

    val claim = JwtClaim(
      expiration = Some(Instant.now.plusSeconds(expireIn).getEpochSecond),
      issuedAt = Some(Instant.now.getEpochSecond),
      subject = Some(s.userId.id),
      issuer = issuer,
      audience = Option(audience).filterNot(_.isEmpty),
      content = Json.toJson(AuthClaimData(r = s.roles, o = s.originUserId.map(_.id))).toString()
    )

    val token = JwtJson.encode(claim, secretKey, algo)

    respondWithHeader(SetAuthorization(token))
  }
}

trait AuthPermissionsDirectives extends AuthDirectives {
  def authService: AuthService

  def permissionsRequired(s: AuthStatus, ps: String*): Directive1[AuthStatus] =
    onSuccess(Future.traverse(s.roles)(authService.getRolePermissions)).flatMap { cps ⇒
      val cs = cps.flatten

      if (ps.forall(cs.contains)) {
        provide(s)
      } else {
        reject(AuthorizationFailedRejection)
      }
    }

  def permissionsRequired(ps: String*)(implicit reqCtx: AuthRequestContext = null): Directive1[AuthStatus] = userRequired.flatMap { s ⇒
    permissionsRequired(s, ps: _*)
  }
}