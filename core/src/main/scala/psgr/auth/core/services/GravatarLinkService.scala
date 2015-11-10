package psgr.auth.core.services

import java.security.MessageDigest

import scala.util.{ Failure, Try }

class GravatarLinkService {

  private lazy val md5 = MessageDigest.getInstance("MD5")

  private val url = "%s://www.gravatar.com/avatar/%s?s=%s&r=%s"
  private val defaultRating = "g"
  private val defaultSize = 400

  val ratings = Set("g", "pg", "r", "x")

  def generateLink(
    email:   String,
    size:    Option[Int]    = None,
    rating:  Option[String] = None,
    secured: Boolean        = true
  ): Try[String] = rating match {
    case Some(r) if ratings.contains(r) ⇒
      Try(url.format(getProtocol(secured), getMD5(email), size.getOrElse(defaultSize).toString, r))
    case Some(r) ⇒
      Failure(new IllegalArgumentException(s"rating can't be $r"))
    case _ ⇒
      Try(url.format(getProtocol(secured), getMD5(email), size.getOrElse(defaultSize).toString, defaultRating))
  }

  private def getMD5(email: String): String = BigInt(1, md5.digest(email.getBytes)).toString(16)

  private def getProtocol(secured: Boolean): String = secured match {
    case true  ⇒ "https"
    case false ⇒ "http"
  }
}