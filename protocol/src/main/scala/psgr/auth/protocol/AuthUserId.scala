package psgr.auth.protocol

import play.api.libs.json.{ JsString, Writes, Reads }

case class AuthUserId(id: String) extends AnyVal

object AuthUserId {
  implicit val r = Reads[AuthUserId] { json ⇒
    json.validate[String].map(id ⇒ AuthUserId(id))
  }

  implicit val w = Writes[AuthUserId] {
    pid ⇒ JsString(pid.id)
  }
}

