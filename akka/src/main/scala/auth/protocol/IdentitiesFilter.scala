package auth.protocol

case class IdentitiesFilter(
  profileId: Option[AuthUserId] = None,
  email:     Option[String]     = None
)
