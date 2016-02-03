package auth.handlers

import auth.api.{ EmailVerifyCommand, ChangeEmailCommand, PasswordRecoverCommand }
import auth.protocol.identities.{ AuthIdentityId, UserIdentitiesFilter, AuthIdentity, AuthIdentitiesList }
import auth.protocol.{ AuthUserId, AuthStatus }
import play.api.libs.json.{ JsString, Json, Writes }

trait AuthJsonWrites {
  implicit val authUserIdWrites = Writes[AuthUserId](id ⇒ JsString(id.id))
  implicit val authStatusEncoder: Writes[AuthStatus] = Json.writes[AuthStatus]
  implicit val authIdentityIdWrites = Writes[AuthIdentityId](id ⇒ JsString(id.id))
  implicit val authIdentityEncoder: Writes[AuthIdentity] = Json.writes[AuthIdentity]
  implicit val userIdentitiesFilterWrites = Json.writes[UserIdentitiesFilter]
  implicit val authIdentitiesListEncoder: Writes[AuthIdentitiesList] = Json.writes[AuthIdentitiesList]

  private[auth] implicit val passwordRecoverCommandWrites = Json.writes[PasswordRecoverCommand]
  private[auth] implicit val changeEmailCommandWrites = Json.writes[ChangeEmailCommand]
  private[auth] implicit val emailVerifyCommandWrites = Json.writes[EmailVerifyCommand]
}

object AuthJsonWrites extends AuthJsonWrites