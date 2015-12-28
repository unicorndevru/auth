package auth.providers.email

import org.mindrot.jbcrypt.{ BCrypt ⇒ B }

object BCryptUtils {

  implicit class Password(val pswrd: String) extends AnyVal {
    def bcrypt: String = B.hashpw(pswrd, generateSalt)

    def bcrypt(rounds: Int): String = B.hashpw(pswrd, B.gensalt(rounds))

    def bcrypt(salt: String): String = B.hashpw(pswrd, salt)

    def isBcrypted(hash: String): Boolean = B.checkpw(pswrd, hash)
  }

  def generateSalt: String = B.gensalt()
}
