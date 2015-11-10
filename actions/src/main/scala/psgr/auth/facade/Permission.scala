package psgr.auth.facade

import play.api.libs.json._

sealed trait Permission {
  def allows(p: Permission): Boolean = p == this
  def flatten(): Seq[Permission] = Permission.perms.values.filter(allows).toSeq
  lazy val name = getClass.getSimpleName.replace("$", "")
}

object Permission {
  val w: Writes[Permission] = Writes{ p ⇒ JsString(p.name) }
  val r: Reads[Permission] = Reads(_.validate[String].map(fromName).flatMap {
    case Some(p) ⇒ JsSuccess(p)
    case None    ⇒ JsError("Permission not found for name")
  })
  implicit val f: Format[Permission] = Format(r, w)

  val perms: Map[String, Permission] = Seq(
    All, Admin, SwitchUser, Carry
  ).map(p ⇒ p.name → p).toMap

  def fromName(name: String): Option[Permission] = perms.get(name)

  case object All extends Permission {
    override def allows(p: Permission): Boolean = true
  }
  case object Admin extends Permission
  case object SwitchUser extends Permission
  case object Carry extends Permission
}