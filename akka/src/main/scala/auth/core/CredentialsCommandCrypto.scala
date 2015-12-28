package auth.core

import scala.util.{ Failure, Success, Try }

trait CredentialsCommandCrypto {
  def encrypt[T <: ExpirableCommand](cmd: T): String

  def decrypt[T <: ExpirableCommand](s: String): Try[T]
}

object CirceBase64UnsafeCommandCrypto extends CredentialsCommandCrypto {
  import io.circe._
  import io.circe.generic.semiauto._
  import io.circe.parse._
  import io.circe.syntax._

  private lazy val encoder = java.util.Base64.getUrlEncoder
  private lazy val decoder = java.util.Base64.getUrlDecoder

  def base64encode(s: String) = encoder.encodeToString(s.getBytes("UTF-8"))

  def base64decode(s: String) = new String(decoder.decode(s), "UTF-8")

  override def encrypt[T <: ExpirableCommand](cmd: T) = base64encode(cmd.asJson.noSpaces)

  override def decrypt[T <: ExpirableCommand](s: String) = decode[T](base64decode(s)).fold(Failure(_), Success(_))
}