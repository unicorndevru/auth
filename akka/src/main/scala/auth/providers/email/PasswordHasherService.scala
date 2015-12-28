package auth.providers.email

import auth.data.identity.PasswordInfo

class PasswordHasherService(val hashers: Set[PasswordHasher], val defaultHasher: PasswordHasher) {
  require(hashers(defaultHasher))

  def createPasswordInfo(password: String, salt: Option[String] = None): PasswordInfo =
    PasswordInfo(hasher = defaultHasher.id, password = defaultHasher.hash(password, salt), salt = salt)

  def validate(password: String, passwordInfo: PasswordInfo): Boolean = {
    hashers.find(_.id == passwordInfo.hasher) match {
      case Some(hasher: PasswordHasher) ⇒ hasher.validate(password, passwordInfo.password)
      case None                         ⇒ false
    }
  }
}

object BCryptPasswordHasherService extends PasswordHasherService(Set(BCryptPasswordHasher), BCryptPasswordHasher)