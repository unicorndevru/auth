package auth.api

import io.circe._
import io.circe.parse._
import io.circe.syntax._
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{ JwtAlgorithm, JwtCirce }

import scala.util.{ Failure, Success, Try }

trait CredentialsCommandCrypto {
  def encrypt[T: Encoder](cmd: T): String

  def decrypt[T: Decoder](s: String): Try[T]
}

object Base64UnsafeCommandCrypto extends CredentialsCommandCrypto {

  private lazy val encoder = java.util.Base64.getUrlEncoder
  private lazy val decoder = java.util.Base64.getUrlDecoder

  def base64encode(s: String) = encoder.encodeToString(s.getBytes("UTF-8"))

  def base64decode(s: String) = new String(decoder.decode(s), "UTF-8")

  override def encrypt[T: Encoder](cmd: T) = base64encode(cmd.asJson.noSpaces)

  override def decrypt[T: Decoder](s: String) = decode[T](base64decode(s)).fold(Failure(_), Success(_))
}

class JwtCommandCrypto(val key: String, val algo: JwtHmacAlgorithm = JwtAlgorithm.HS256) extends CredentialsCommandCrypto {

  override def encrypt[T: Encoder](cmd: T) = JwtCirce.encode(cmd.asJson, key, algo)

  override def decrypt[T: Decoder](s: String) = JwtCirce.decodeJson(s, key, Seq(algo)).flatMap(_.as[T].fold(Failure(_), Success(_)))

}