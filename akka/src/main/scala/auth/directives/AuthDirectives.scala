package auth.directives

import java.time.Instant

import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{ AuthorizationFailedRejection, Directive0, Directive1 }
import auth.protocol.{ AuthError, AuthStatus, AuthUserId }
import auth.services.AuthService
import io.circe._
import io.circe.generic.semiauto._
import io.circe.jawn.{ parse ⇒ jawnParse }
import io.circe.syntax._
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{ JwtAlgorithm, JwtCirce, JwtClaim }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class AuthParams(
  secretKey: String,
  expireIn:  Int              = 86400,
  issuer:    Option[String]   = Some("auth"),
  audience:  Option[String]   = None,
  algo:      JwtHmacAlgorithm = JwtAlgorithm.HS256
)

case class AuthClaimData(
  r: Seq[String],
  o: Option[String]
)

object AuthClaimData {
  implicit val decoder: Decoder[AuthClaimData] = deriveFor[AuthClaimData].decoder
  implicit val encoder: Encoder[AuthClaimData] = deriveFor[AuthClaimData].encoder
}

trait AuthDirectives {
  val authParams: AuthParams

  import authParams._

  val userAware: Directive1[Option[AuthStatus]] = optionalHeaderValueByType(classOf[Authorization]).flatMap{
    case Some(a) ⇒
      provide(a.credentials match {
        case t: OAuth2BearerToken ⇒
          JwtCirce.decode(t.token, secretKey, Seq(JwtAlgorithm.HS256))
            .filter(claim ⇒
              issuer.fold(claim.isValid)(iss ⇒
                audience.fold(claim.isValid(iss))(aud ⇒ claim.isValid(iss, aud)))
                && claim.subject.isDefined)
            .toOption
            .map { claim ⇒
              val s = AuthStatus(
                userId = AuthUserId(claim.subject.get),
                roles = Seq.empty,
                originUserId = None
              )

              jawnParse(claim.content).flatMap(_.as[AuthClaimData]).fold(
                _ ⇒ s,
                d ⇒ s.copy(
                  roles = d.r,
                  originUserId = d.o.map(AuthUserId)
                )
              )
            }

        case t ⇒
          None
      })
    case None ⇒
      provide(None)
  }

  val userRequired: Directive1[AuthStatus] = userAware.flatMap {
    case Some(a) ⇒ provide(a)
    case None    ⇒ failWith(AuthError.Unauthorized)
  }

  def rolesRequired(s: AuthStatus, rs: String*): Directive1[AuthStatus] =
    if (rs.forall(rs.contains)) {
      provide(s)
    } else {
      reject(AuthorizationFailedRejection)
    }

  def rolesRequired(rs: String*): Directive1[AuthStatus] = userRequired.flatMap { s ⇒
    rolesRequired(s, rs: _*)
  }

  def respondWithAuth: Directive0 = userAware.flatMap {
    case Some(a) ⇒ respondWithAuth(a)
    case None    ⇒ pass
  }

  def respondWithAuth(s: AuthStatus): Directive0 = {

    val claim = JwtClaim(
      expiration = Some(Instant.now.plusSeconds(expireIn).getEpochSecond),
      issuedAt = Some(Instant.now.getEpochSecond),
      subject = Some(s.userId.id),
      issuer = issuer,
      audience = audience,
      content = AuthClaimData(r = s.roles.distinct, o = s.originUserId.map(_.id)).asJson.noSpaces
    )

    val token = JwtCirce.encode(claim, secretKey, algo)

    respondWithHeader(Authorization(OAuth2BearerToken(token)))
  }
}

trait AuthPermissionsDirectives extends AuthDirectives {
  def authService: AuthService

  def permissionsRequired(s: AuthStatus, ps: String*): Directive1[AuthStatus] =
    onSuccess(Future.traverse(s.roles)(authService.getRolePermissions)).flatMap { cps ⇒
      val cs = cps.flatten.toSet

      if (ps.forall(cs.contains)) {
        provide(s)
      } else {
        reject(AuthorizationFailedRejection)
      }
    }

  def permissionsRequired(ps: String*): Directive1[AuthStatus] = userRequired.flatMap { s ⇒
    permissionsRequired(s, ps: _*)
  }
}