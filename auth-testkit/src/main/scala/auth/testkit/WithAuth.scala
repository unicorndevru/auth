package auth.testkit

import java.time.Instant

import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import auth.directives.AuthClaimData._
import auth.directives.{AuthClaimData, AuthParams}
import auth.protocol.AuthStatus
import io.circe.syntax._
import pdi.jwt.{JwtCirce, JwtClaim}

abstract class WithAuth(status: AuthStatus, params: AuthParams) {

  import params._

  val claim = JwtClaim(
    expiration = Some(Instant.now.plusSeconds(expireIn).getEpochSecond),
    issuedAt = Some(Instant.now.getEpochSecond),
    subject = Some(status.userId.id),
    issuer = issuer,
    audience = audience,
    content = AuthClaimData(r = status.roles.distinct, o = status.originUserId.map(_.id)).asJson.noSpaces
  )

  val token = JwtCirce.encode(claim, secretKey, algo)

  val tokenHeader: Authorization = Authorization(OAuth2BearerToken(token))
}
