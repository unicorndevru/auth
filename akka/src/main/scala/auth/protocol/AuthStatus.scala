package auth.protocol

case class AuthStatus(
    userId:       AuthUserId,
    roles:        Set[String],
    originUserId: Option[AuthUserId]
) {
  def hasRole(r: String) = roles(r) || roles.exists(_ equalsIgnoreCase r)
}