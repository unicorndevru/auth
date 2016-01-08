package auth.protocol.identities

case class AuthIdentitiesList(
  filter: UserIdentitiesFilter,
  items:  Seq[AuthIdentity]
)