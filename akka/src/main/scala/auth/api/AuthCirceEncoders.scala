package auth.api

import auth.protocol.AuthStatus

import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.generic.semiauto._

trait AuthCirceEncoders {
  implicit val authStatusEncoder: Encoder[AuthStatus] = deriveFor[AuthStatus].encoder
}
