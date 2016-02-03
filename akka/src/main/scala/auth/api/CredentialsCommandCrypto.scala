package auth.api

import auth.protocol.AuthError.JsonParseError
import play.api.libs.json.{ JsError, Json, Reads, Writes }

import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{ JwtJson, JwtAlgorithm }

import scala.util.{ Failure, Success, Try }

trait CredentialsCommandCrypto {
  def encrypt[T: Writes](cmd: T): String

  def decrypt[T: Reads](s: String): Try[T]
}

object Base64UnsafeCommandCrypto extends CredentialsCommandCrypto {

  private lazy val encoder = java.util.Base64.getUrlEncoder
  private lazy val decoder = java.util.Base64.getUrlDecoder

  def base64encode(s: String) = encoder.encodeToString(s.getBytes("UTF-8"))

  def base64decode(s: String) = new String(decoder.decode(s), "UTF-8")

  override def encrypt[T: Writes](cmd: T) = base64encode(Json.toJson(cmd).toString)

  override def decrypt[T: Reads](s: String) = Try(Json.parse(base64decode(s)).validate[T].get)
}

class JwtCommandCrypto(val key: String, val algo: JwtHmacAlgorithm = JwtAlgorithm.HS256) extends CredentialsCommandCrypto {

  override def encrypt[T: Writes](cmd: T) = JwtJson.encode(Json.toJson(cmd).toString(), key, algo)

  override def decrypt[T: Reads](s: String) =
    JwtJson.decodeJson(s, key, Seq(algo))
      .flatMap(o ⇒ o.validate[T].asEither.fold[Try[T]](fa ⇒ Failure(JsonParseError(JsError.toJson(fa))), t ⇒ Success(t)))

}