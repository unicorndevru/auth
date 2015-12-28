package auth.protocol

case class AuthStatus(
  user:       AuthUserId,
  roles:      Seq[String],
  isSwitched: Option[Boolean]
)