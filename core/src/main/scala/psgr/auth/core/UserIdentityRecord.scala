package psgr.auth.core

import psgr.auth.protocol.AuthUserId
import reactivemongo.bson._

/**
 * @author alari
 * @since 7/3/13 10:08 PM
 */
private[auth] case class UserIdentityRecord(
    identityId: IdentityId,

    firstName: String,
    lastName:  String,
    fullName:  String,

    _id: Option[BSONObjectID] = Some(BSONObjectID.generate),

    profileId: Option[AuthUserId] = None,

    email:           Option[String]  = None,
    isEmailVerified: Option[Boolean] = None,
    avatarUrl:       Option[String]  = None,

    authMethod:   AuthenticationMethod,
    oAuth1Info:   Option[OAuth1Info]   = None,
    oAuth2Info:   Option[OAuth2Info]   = None,
    passwordInfo: Option[PasswordInfo] = None,

    locale: Option[String] = None
) {

  def toLowerCased = copy(identityId = identityId.copy(userId = identityId.userId.toLowerCase), email = email.map(_.toLowerCase))
}

object UserIdentityRecord {

  implicit val authUserIdFormat = new BSONWriter[AuthUserId, BSONString] with BSONReader[BSONString, AuthUserId] {
    override def write(t: AuthUserId) = BSONString(t.id)

    override def read(bson: BSONString) = AuthUserId(bson.value)
  }
  implicit val authenticationMethodFormat = Macros.handler[AuthenticationMethod]
  implicit val oAuth1Format = Macros.handler[OAuth1Info]
  implicit val oAuth2Format = Macros.handler[OAuth2Info]
  implicit val passwordInfoFormat = Macros.handler[PasswordInfo]
  implicit val identityIdFormat = Macros.handler[IdentityId]

  implicit val format = Macros.handler[UserIdentityRecord]

}