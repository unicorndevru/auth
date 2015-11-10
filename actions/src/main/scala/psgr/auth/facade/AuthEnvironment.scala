package psgr.auth.facade

import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{ RequestHeader, Result, Results }
import psgr.failures.JsonApiFailure

class AuthEnvironment @Inject() (val jwtAuthenticator: JwtAuthenticator, val permissionsService: AuthPermissionsService) {
  def unauthorizedResponse(rh: RequestHeader): Result = Results.Unauthorized(Json.toJson(JsonApiFailure(401, "unauthorized", "Unauthorized", "user")))

  def forbiddenResponse(rh: RequestHeader): Result = Results.Forbidden(Json.toJson(JsonApiFailure(403, "forbidden", "Forbidden", "user")))
}
