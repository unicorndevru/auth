package auth.protocol

case class AuthIdentitiesList(
  filter: IdentitiesFilter,
  items:  Seq[AuthIdentity]
)