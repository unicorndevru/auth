package auth.api

import auth.directives.AuthParams
import com.typesafe.config.ConfigFactory

import scala.util.Try

trait AuthCryptoConfig {
  def authParams: AuthParams
  def credentialsCommandCrypto: CredentialsCommandCrypto
}

trait DefaultAuthCryptoConfig extends AuthCryptoConfig {

  private lazy val config = ConfigFactory.load()

  lazy val authParams = AuthParams(
    secretKey = config.getString("auth.secretKey"),
    expireIn = Try(config.getInt("auth.expireIn")).getOrElse(86400),
    issuer = Try(config.getString("auth.issuer")).toOption,
    audience = Try(config.getString("auth.audience")).toOption
  )

  lazy val credentialsCommandCrypto: CredentialsCommandCrypto =
    new JwtCommandCrypto(Try(config.getString("auth.cryptoKey")).getOrElse(authParams.secretKey))
}