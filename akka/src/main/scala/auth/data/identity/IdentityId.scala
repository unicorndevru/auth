package auth.data.identity

/**
 * The ID of an Identity
 *
 * @param userId the user id on the provider the user came from (eg: twitter, facebook)
 * @param providerId the provider used to sign in
 */
case class IdentityId(userId: String, providerId: String)