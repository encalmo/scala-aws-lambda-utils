package org.encalmo.lambda

import sttp.client4.{Request, Response, StringBody}
import ujson.Obj
import upickle.default.{ReadWriter, read, writeJs}

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.io.AnsiColor.*
import scala.util.Try
import scala.util.control.NonFatal

object Utils {

  val rfcDateTime = DateTimeFormatter
    .ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)

  def rfcDateTimeNow() = rfcDateTime.format(ZonedDateTime.now())

  extension [T, R <: Request[T]](request: R)
    def printDebug(debug: String => Unit): R = {
      debug(
        s"""[HttpClient] ${BOLD}${MAGENTA}${request.method.method} ${request.uri.toString}${RESET}${MAGENTA} ${request.headers
            .map(h => s"${h.name}:${h.value}")
            .mkString("  ")} body: ${BLUE}${request.body.match {
            case s: StringBody => s.s; case o => o.show
          }}${RESET}""".stripMargin
      )
      request
    }

  extension (response: Response[String])
    def wrapInApiGatewayResponse(): ApiGatewayResponse = {
      ApiGatewayResponse(
        statusCode = response.code.code,
        headers = response.headers
          .map(h => (h.name, h.value))
          .filterNot { (name, _) =>
            val n = name.toLowerCase()
            n == "content-encoding"
          }
          .toMap,
        isBase64Encoded = false,
        body = response.body.toString()
      )
    }

    def printDebug(debug: String => Unit): Response[String] = {
      debug(
        s"""[HttpClient] ${BOLD}${MAGENTA}${response.code} ${response.statusText}${RESET}${MAGENTA} ${response.headers
            .map(h => s"${h.name}:${h.value}")
            .mkString(
              "  "
            )} body: ${BLUE}${response.body}${RESET}""".stripMargin
      )
      response
    }

  extension [L, R](value: Either[L, R]) {
    inline def tapRight(f: R => Unit): value.type = {
      value.foreach(f)
      value
    }

    inline def tapLeft(f: L => Unit): value.type = {
      value.left.foreach(f)
      value
    }

    inline def tap(f: Either[L, R] => Unit): value.type = {
      f(value)
      value
    }

    inline def forget: Either[L, Unit] =
      value.map(_ => ())
  }

  extension [T](value: Option[T]) {

    inline def tap(f: Option[T] => Unit): value.type = {
      f(value)
      value
    }

    inline def tapSome(f: T => Unit): value.type = {
      value.foreach(f)
      value
    }

    inline def tapNone(f: => Unit): value.type = {
      if (value.isEmpty) f
      value
    }
  }

  extension [T](value: Either[Error, Option[T]]) {
    inline def flatMapEO[R](
        f: T => Either[Error, Option[R]]
    ): Either[Error, Option[R]] =
      value.fold(
        error => Left(error),
        {
          case None => Right(None)
          case Some(value) =>
            f(value)
        }
      )

    inline def flatMapE[R](f: T => Either[Error, R]): Either[Error, Option[R]] =
      value.fold(
        error => Left(error),
        {
          case None => Right(None)
          case Some(value) =>
            f(value).map(Some.apply)
        }
      )
  }

  extension [T](value: Either[Throwable, T])
    inline def eitherErrorOrUnit: Either[Error, Unit] =
      value
        .map(_ => ())
        .left
        .map(Error.from)

    inline def eitherErrorOrResult: Either[Error, T] =
      value.left.map(Error.from)

    inline def eitherErrorOr[R](result: R): Either[Error, R] =
      value.map(_ => result).left.map(Error.from)

  extension [T](value: Either[Throwable, Either[Error, T]])
    inline def eitherErrorOrResultFlatten: Either[Error, T] =
      value.left
        .map(Error.from)
        .flatten
}
