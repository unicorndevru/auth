package auth.data.identity

sealed trait AccessInfo {
  def method: AuthenticationMethod = this match {
    case _: OAuth1Info   ⇒ AuthenticationMethod.OAuth1
    case _: OAuth2Info   ⇒ AuthenticationMethod.OAuth2
    case _: PasswordInfo ⇒ AuthenticationMethod.UserPassword
  }
}

/**
 * The OAuth 1 details
 *
 * @param token the token
 * @param secret the secret
 */
case class OAuth1Info(token: String, secret: String) extends AccessInfo

/**
 * The Oauth2 details
 *
 * @param accessToken the access token
 * @param tokenType the token type
 * @param expiresIn the number of seconds before the token expires
 * @param refreshToken the refresh token
 */
case class OAuth2Info(accessToken: String, tokenType: Option[String] = None,
                      expiresIn: Option[Int] = None, refreshToken: Option[String] = None) extends AccessInfo

/**
 * The password details
 *
 * @param password the hashed password
 */
case class PasswordInfo(hasher: String, password: String, salt: Option[String] = None) extends AccessInfo