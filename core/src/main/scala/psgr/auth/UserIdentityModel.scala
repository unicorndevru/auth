package psgr.auth

import play.api.libs.json.Json
import psgr.auth.core._
import psgr.auth.core.identity._
import psgr.auth.protocol.{ AuthIdentityId, AuthIdentity, AuthUserId }
import reactivemongo.bson.BSONObjectID
import scala.language.implicitConversions

case class UserIdentityModel(
  id:         String     = BSONObjectID.generate.stringify,
  identityId: IdentityId,

  firstName: String,
  lastName:  String,
  fullName:  String,

  profileId: Option[AuthUserId] = None,

  email:           Option[String]  = None,
  isEmailVerified: Option[Boolean] = None,
  avatarUrl:       Option[String]  = None,

  authMethod:   AuthenticationMethod,
  oAuth1Info:   Option[OAuth1Info]   = None,
  oAuth2Info:   Option[OAuth2Info]   = None,
  passwordInfo: Option[PasswordInfo] = None,

  locale: Option[String] = None
)

object UserIdentityModel {
  implicit val authenticationMethodFormat = Json.format[AuthenticationMethod]
  implicit val oAuth1Format = Json.format[OAuth1Info]
  implicit val oAuth2Format = Json.format[OAuth2Info]
  implicit val passwordInfoFormat = Json.format[PasswordInfo]
  implicit val identityIdFormat = Json.format[IdentityId]

  implicit val format = Json.format[UserIdentityModel]

  implicit def recordToModel(userIdentityRecord: UserIdentityRecord): UserIdentityModel = UserIdentityModel(
    id = userIdentityRecord._id.map(_.stringify).getOrElse(BSONObjectID.generate.stringify),
    identityId = userIdentityRecord.identityId,
    firstName = userIdentityRecord.firstName,
    lastName = userIdentityRecord.lastName,
    fullName = userIdentityRecord.fullName,

    profileId = userIdentityRecord.profileId,

    email = userIdentityRecord.email,
    isEmailVerified = userIdentityRecord.isEmailVerified,
    avatarUrl = userIdentityRecord.avatarUrl,

    authMethod = userIdentityRecord.authMethod,
    oAuth1Info = userIdentityRecord.oAuth1Info,
    oAuth2Info = userIdentityRecord.oAuth2Info,
    passwordInfo = userIdentityRecord.passwordInfo,

    locale = userIdentityRecord.locale
  )

  implicit def modelToRecord(userIdentityModel: UserIdentityModel): UserIdentityRecord = UserIdentityRecord(
    _id = BSONObjectID.parse(userIdentityModel.id).toOption,
    identityId = userIdentityModel.identityId,
    firstName = userIdentityModel.firstName,
    lastName = userIdentityModel.lastName,
    fullName = userIdentityModel.fullName,

    profileId = userIdentityModel.profileId,

    email = userIdentityModel.email,
    isEmailVerified = userIdentityModel.isEmailVerified,
    avatarUrl = userIdentityModel.avatarUrl,

    authMethod = userIdentityModel.authMethod,
    oAuth1Info = userIdentityModel.oAuth1Info,
    oAuth2Info = userIdentityModel.oAuth2Info,
    passwordInfo = userIdentityModel.passwordInfo,

    locale = userIdentityModel.locale
  )

  implicit def modelToProtocol(model: UserIdentityModel): AuthIdentity = AuthIdentity(
    id = model.id,
    identityId = AuthIdentityId(id = model.identityId.userId, provider = model.identityId.providerId),
    email = model.email,
    isEmailVerified = model.isEmailVerified
  )
}