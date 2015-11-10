package psgr.auth.core.providers

trait PasswordHasher {
  val id: String
  def validate(password: String, hash: String): Boolean
  def hash(password: String, salt: Option[String] = None): String
}

class BCryptPasswordHasher extends PasswordHasher {
  import com.github.t3hnar.bcrypt._

  val id = "bcrypt"

  def validate(password: String, hash: String): Boolean = password.isBcrypted(hash)

  def hash(password: String, salt: Option[String] = None): String = salt match {
    case Some(saltString) ⇒ password.bcrypt(saltString)
    case None             ⇒ password.bcrypt
  }

}
