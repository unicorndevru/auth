package psgr.auth.core.services

import java.net.URLEncoder
import javax.inject.Inject

import play.api.libs.Crypto
import play.api.libs.json.{ Json, Reads, Writes }

import scala.util.Try

class CommandCryptoService @Inject() (crypto: Crypto) {

  private lazy val defaultCodec = "UTF-8"

  def encryptCommand[T: Writes](cmd: T, codec: String = defaultCodec): String =
    URLEncoder
      .encode(
        crypto
          .encryptAES(Json.toJson(cmd).toString()), codec
      )

  def decryptToken[T: Reads](token: String): Try[T] =
    for {
      str ← Try(crypto.decryptAES(token))
      json ← Try(Json.parse(str))
      Some(v) = json.validate[T].asOpt
    } yield v
}