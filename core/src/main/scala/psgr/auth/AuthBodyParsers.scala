package psgr.auth

import javax.inject.Inject

import play.api.libs.json.{ JsError, Json, Reads }
import play.api.mvc.BodyParsers.parse
import play.api.mvc.{ BodyParser, Results }
import psgr.auth.core.services.{ CommandCryptoService, ExpirableCommand }
import psgr.auth.protocol.TokenCommand
import psgr.failures.JsonApiFailure

import scala.concurrent.Future
import scala.util.Success

import play.api.libs.concurrent.Execution.Implicits.defaultContext

case class BodyJson[C, T](json: C, token: T)

class AuthBodyParsers @Inject() (cryptoService: CommandCryptoService) {

  private def wrongInputFailure(jsError: JsError) = JsonApiFailure(400, "wrong_input", "Please fill all the fields with appropriate data", "auth", Some(JsError.toJson(jsError)))
  private def wrongInputResult(jsError: JsError) = Results.BadRequest(Json.toJson(wrongInputFailure(jsError)))

  private val cannotParseTokenFailure = JsonApiFailure(400, "cannot_parse_token", "Cannot parse token string", "auth")
  private val cannotParseTokenResult = Results.BadRequest(Json.toJson(cannotParseTokenFailure))

  private val tardyTokenFailure = JsonApiFailure(400, "tardy_token", "Too late my friend", "auth")
  private val tardyTokenResult = Results.BadRequest(Json.toJson(tardyTokenFailure))

  private lazy val millisInDay = 24 * 60 * 60 * 1000

  def authBodyJson[T: Reads]: BodyParser[T] =
    BodyParser("json service reader") { request ⇒
      parse.json(request) mapM {
        case Left(simpleResult) ⇒
          Future.successful(Left(simpleResult))
        case Right(jsValue) ⇒
          jsValue.validate(implicitly[Reads[T]]) map { a ⇒
            Future.successful(Right(a))
          } recoverTotal { jsError ⇒
            Future.successful(Left(wrongInputResult(jsError)))
          }
      }
    }

  def authTokenJson[C <: TokenCommand: Reads, T: Reads]: BodyParser[BodyJson[C, T]] =
    authBodyJson[C]
      .map(cmd ⇒ cryptoService.decryptToken[T](cmd.token).map(cmd → _))
      .validate {
        case Success((cmd, t)) ⇒ Right(BodyJson(cmd, t))
        case _                 ⇒ Left(cannotParseTokenResult)
      }

  def authToken[C <: TokenCommand: Reads, T: Reads]: BodyParser[T] =
    authTokenJson[C, T].map(_.token)

  def authTokenExpirableJson[C <: TokenCommand: Reads, T <: ExpirableCommand: Reads](expireMillis: Long = millisInDay): BodyParser[BodyJson[C, T]] =
    authTokenJson[C, T].validate {
      case t if t.token.isExpired(expireMillis) ⇒ Left(tardyTokenResult)
      case t                                    ⇒ Right(t)
    }

  def authTokenExpirable[C <: TokenCommand: Reads, T <: ExpirableCommand: Reads](expireMillis: Long = millisInDay) =
    authTokenExpirableJson[C, T](expireMillis).map(_.token)
}
