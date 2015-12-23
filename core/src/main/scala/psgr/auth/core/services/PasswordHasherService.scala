package psgr.auth.core.services

import javax.inject.Inject
import psgr.auth.core.identity.PasswordInfo
import psgr.auth.core.providers.{ PasswordHasher, BCryptPasswordHasher }

class PasswordHasherService @Inject() (bCryptPasswordHasher: BCryptPasswordHasher) {
  lazy val hashers = Set(bCryptPasswordHasher)
  lazy val currentHasher = bCryptPasswordHasher

  def createPasswordInfo(password: String, salt: Option[String] = None): PasswordInfo =
    PasswordInfo(hasher = currentHasher.id, password = currentHasher.hash(password, salt), salt = salt)

  def validate(password: String, passwordInfo: PasswordInfo): Boolean = {
    hashers.find(_.id == passwordInfo.hasher) match {
      case Some(hasher: PasswordHasher) ⇒ hasher.validate(password, passwordInfo.password)
      case None                         ⇒ false
    }
  }
}