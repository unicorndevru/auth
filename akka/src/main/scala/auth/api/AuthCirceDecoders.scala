package auth.api

import auth.protocol.{ AuthByCredentials, AuthByToken }
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.generic.semiauto._

trait AuthCirceDecoders {
  implicit val authByTokenDecoder: Decoder[AuthByToken] = deriveFor[AuthByToken].decoder
  implicit val authByCredentialsDecoder: Decoder[AuthByCredentials] = deriveFor[AuthByCredentials].decoder
}
