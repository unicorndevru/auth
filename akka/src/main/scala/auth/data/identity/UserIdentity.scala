package auth.data.identity

import java.time.LocalDate

import auth.protocol.AuthUserId

case class UserIdentity(
  identityId: IdentityId,

  firstName: Option[String],
  lastName:  Option[String],
  fullName:  Option[String],

  _id: Option[String] = None,

  userId: Option[AuthUserId] = None,

  email:           Option[String]  = None,
  isEmailVerified: Option[Boolean] = None,
  avatarUrl:       Option[String]  = None,

  authMethod:   AuthenticationMethod,
  oAuth1Info:   Option[OAuth1Info]   = None,
  oAuth2Info:   Option[OAuth2Info]   = None,
  passwordInfo: Option[PasswordInfo] = None,

  locale: Option[String] = None,

  birthday: Option[LocalDate] = None
)
