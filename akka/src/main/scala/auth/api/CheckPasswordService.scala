package auth.api

trait CheckPasswordService {
  def isStrongEnough(password: String): Boolean
}

object CheckPasswordService extends CheckPasswordService {
  override def isStrongEnough(password: String) = password.trim.length > 3
}