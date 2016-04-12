package auth.mongo.identities

import auth.data.identity._
import auth.protocol.AuthUserId
import reactivemongo.bson._

private[mongo] case class UserIdentityRecord(
    identityId: IdentityId,

    firstName: Option[String],
    lastName:  Option[String],
    fullName:  Option[String],

    _id: BSONObjectID = BSONObjectID.generate,

    userId: Option[AuthUserId] = None,

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

private[mongo] object UserIdentityRecord {

  implicit val authUserIdFormat = new BSONWriter[AuthUserId, BSONString] with BSONReader[BSONString, AuthUserId] {
    override def write(t: AuthUserId) = BSONString(t.id)

    override def read(bson: BSONString) = AuthUserId(bson.value)
  }
  implicit val authenticationMethodFormat = Macros.handler[AuthenticationMethod]
  implicit val oAuth1Format = Macros.handler[OAuth1Info]
  implicit val oAuth2Format = Macros.handler[OAuth2Info]
  implicit val passwordInfoFormat = Macros.handler[PasswordInfo]
  implicit val identityIdFormat = Macros.handler[IdentityId]

  implicit val format = {
    // TODO: remove legacy format conversion
    val h = Macros.handler[UserIdentityRecord]
    new BSONDocumentReader[UserIdentityRecord] with BSONDocumentWriter[UserIdentityRecord] {
      override def read(bson: BSONDocument) = {
        h.read(bson.getAs[BSONString]("profileId").fold(bson)(pid ⇒ bson ++ ("userId" → pid)))
      }

      override def write(t: UserIdentityRecord) = h.write(t)
    }
  }

}
