package auth.handlers

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import utils.http.json.{ JsonMarshallingContext, PlayJsonSupport }

trait AuthHandlerJson extends PlayJsonSupport with AuthJsonWrites with AuthJsonReads {
  def extractJsonMarshallingContext: Directive1[JsonMarshallingContext] = provide(null)
}
