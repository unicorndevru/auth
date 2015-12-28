package auth.protocol

case class AuthStatus(
  userId:     AuthUserId,
  roles:      Seq[String],
  isSwitched: Option[Boolean]
)