package auth.testkit

import java.time.Instant

import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }
import auth.directives.AuthClaimData._
import auth.directives.{ AuthClaimData, AuthParams }
import auth.protocol.AuthStatus
import pdi.jwt.{ JwtClaim, JwtJson }
import play.api.libs.json.Json

abstract class WithAuth(status: AuthStatus, params: AuthParams) {

  import params._

  val claim = JwtClaim(
    expiration = Some(Instant.now.plusSeconds(expireIn).getEpochSecond),
    issuedAt = Some(Instant.now.getEpochSecond),
    subject = Some(status.userId.id),
    issuer = issuer,
    audience = audience,
    content = Json.toJson(AuthClaimData(r = status.roles, o = status.originUserId.map(_.id))).toString
  )

  val token = JwtJson.encode(claim, secretKey, algo)

  val tokenHeader: Authorization = Authorization(OAuth2BearerToken(token))
}
