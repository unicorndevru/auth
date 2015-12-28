package auth.data.identity

/**
 * A class representing an authentication method
 */
case class AuthenticationMethod(method: String) {
  /**
   * Returns true if this authentication method equals another. Eg: user.authMethod.is(AuthenticationMethod.OAuth1)
   *
   * @param m An Authentication Method (see constants)
   * @return true if the method matches, false otherwise
   */
  def is(m: AuthenticationMethod): Boolean = this == m
}

/**
 * Authentication methods used by the identity providers
 */
object AuthenticationMethod {
  val OAuth1 = AuthenticationMethod("oauth1")
  val OAuth2 = AuthenticationMethod("oauth2")
  val UserPassword = AuthenticationMethod("userPassword")
}
