package auth.handlers

import auth.protocol.identities.{ AuthIdentity, AuthIdentitiesList }
import auth.protocol.AuthStatus
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto._

trait AuthCirceEncoders {
  implicit val authStatusEncoder: Encoder[AuthStatus] = deriveFor[AuthStatus].encoder
  implicit val authIdentityEncoder: Encoder[AuthIdentity] = deriveFor[AuthIdentity].encoder
  implicit val authIdentitiesListEncoder: Encoder[AuthIdentitiesList] = deriveFor[AuthIdentitiesList].encoder
}
