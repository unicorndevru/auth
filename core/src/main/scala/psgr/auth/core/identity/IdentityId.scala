package psgr.auth.core.identity

import play.api.libs.json.Json

/**
 * The ID of an Identity
 *
 * @param userId the user id on the provider the user came from (eg: twitter, facebook)
 * @param providerId the provider used to sign in
 */
private[auth] case class IdentityId(userId: String, providerId: String)

object IdentityId {
  implicit val format = Json.format[IdentityId]
}
