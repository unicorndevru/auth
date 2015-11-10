package psgr.auth.facade

import javax.inject.Inject

import pdi.jwt.{ JwtClaim, JwtPlayImplicits, JwtSession }
import play.api.Configuration
import play.api.libs.json.JsString
import play.api.mvc.{ RequestHeader, Result, Results }
import psgr.auth.protocol.AuthUserId

class JwtAuthenticator @Inject() (config: Configuration) extends JwtPlayImplicits {
  val PlaySessionKey = "jwt"
  val SwitchedSubjectKey = "origin"

  val cookiesEnabled = config.getBoolean("psgr.auth.useCookies").getOrElse(false)
  val issuer = config.getString("psgr.main.domain").getOrElse("psgr")

  def deserializeBearer(tokenBase: String): JwtSession = JwtSession.deserialize(sanitizeBearer(tokenBase))

  def sanitizeBearer(tokenBase: String): String = if (tokenBase.startsWith(JwtSession.TOKEN_PREFIX)) {
    tokenBase.substring(JwtSession.TOKEN_PREFIX.length()).trim
  } else {
    tokenBase.trim
  }

  def profileFromRequest(request: RequestHeader): Option[AuthUserId] = {
    val ses = request.jwtSession
    val cookieSes = request.session.get(PlaySessionKey).filter(_ ⇒ cookiesEnabled).map(JwtSession.deserialize)
    val s = if (ses.claim.subject.isEmpty) cookieSes.getOrElse(ses) else ses
    s.claim.subject.map(AuthUserId(_))
  }

  def refreshJwt(rh: RequestHeader)(result: Result): Result = result.header.headers.get(JwtSession.HEADER_NAME).orElse(rh.headers.get(JwtSession.HEADER_NAME)) match {
    case Some(t) ⇒
      val s = deserializeBearer(t).refresh
      val r = result.withJwtSession(s)

      if (cookiesEnabled) r.withSession(PlaySessionKey → s.serialize) else r

    case None ⇒ result
  }

  def authorize(profileId: AuthUserId)(result: Result): Result = {
    val s = JwtSession(JwtClaim(issuer = Some(issuer), subject = Some(profileId.id), issuedAt = Some(System.currentTimeMillis() / 1000))).refresh
    val rj = result.withJwtSession(s)
    if (cookiesEnabled) rj.withSession(PlaySessionKey → s.serialize) else rj
  }

  def unbecome(result: Result)(implicit rh: RequestHeader): Result = {
    val session = result.jwtSession
    result withJwtSession session.getAs[String](SwitchedSubjectKey).fold(session)(sub ⇒ session.withClaim(session.claim.copy(subject = Some(sub))) - SwitchedSubjectKey)
  }

  def unbecome(implicit rh: RequestHeader): Result = {
    if (isSwitched) unbecome(Results.Ok) else Results.NoContent
  }

  def become(profileId: AuthUserId, result: Result)(implicit requestHeader: RequestHeader): Result = {
    val session = result.jwtSession
    result withJwtSession session.claim.subject.fold(session)(sub ⇒ session.withClaim(session.claim.copy(subject = Some(profileId.id))) ++ (SwitchedSubjectKey → JsString(sub)))
  }

  def isSwitched(implicit request: RequestHeader): Boolean = {
    request.jwtSession.getAs[String](SwitchedSubjectKey).isDefined
  }

  def clean(result: Result): Result = result.copy(header = result.header.copy(headers = result.header.headers - JwtSession.HEADER_NAME))
}
