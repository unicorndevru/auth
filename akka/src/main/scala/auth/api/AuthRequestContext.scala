package auth.api

import akka.http.scaladsl.model.RemoteAddress
import auth.protocol.AuthStatus

trait AuthRequestContext {
  def remoteAddress: Option[RemoteAddress]
  def userAgent: Option[String]
  def status: Option[AuthStatus]
}