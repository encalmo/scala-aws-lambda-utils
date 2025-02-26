package org.encalmo.lambda

import org.encalmo.lambda.Utils.*

import sttp.client4.quick.*
import sttp.client4.{Request, Response}
import sttp.model.{Method, Uri}
import upickle.default.*

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import scala.io.AnsiColor.*

final case class ApiGatewayRequest(
    httpMethod: String,
    resource: String,
    path: String,
    body: String,
    isBase64Encoded: Boolean,
    headers: Map[String, String],
    stageVariables: Map[String, String],
    queryStringParameters: Map[String, String],
    requestContext: Map[String, ujson.Value]
) {

  inline def getResourceOrPathIfProxy: String =
    if (resource.contains("{proxy+}")) path else resource

  inline def replacePathWithResourceIfNotProxy: ApiGatewayRequest =
    copy(path = getResourceOrPathIfProxy)

  private lazy val canonicalHeaders =
    Option(headers)
      .map(_.map((k, v) => (k.toLowerCase(), v)))
      .getOrElse(Map.empty)

  inline def parseBodyAs[T: ReadWriter]: T =
    try (body.readAs[T])
    catch {
      case e: Exception =>
        throw new ApiGatewayRequestBodyParseException(e)
    }

  inline def withPath(path: String): ApiGatewayRequest =
    copy(path = path)

  inline def modifyPath(f: String => String): ApiGatewayRequest =
    copy(path = f(path))

  inline def withHeader(name: String, value: String): ApiGatewayRequest =
    copy(headers = Option(headers).getOrElse(Map.empty) + (name -> value))

  inline def withQueryParameter(
      name: String,
      value: String
  ): ApiGatewayRequest =
    copy(queryStringParameters = Option(queryStringParameters).getOrElse(Map.empty) + (name -> value))

  inline def maybeHeader(name: String): Option[String] =
    canonicalHeaders.get(name.toLowerCase())

  inline def maybeHeader(name: String, defaultValue: String): String =
    canonicalHeaders.get(name.toLowerCase()).getOrElse(defaultValue)

  private lazy val urlEncodedForm: Map[String, String] = {
    val form =
      if (
        maybeHeader("Content-Type")
          .exists(_.startsWith("application/x-www-form-urlencoded"))
      )
      then
        body
          .split("&")
          .map(_.split("="))
          .filter(_.nonEmpty)
          .map(a =>
            if (a.length >= 2) then (a(0), URLDecoder.decode(a(1), StandardCharsets.UTF_8))
            else (a(0), "")
          )
          .toMap
      else Map.empty
    form
  }

  inline def formDebugString: String =
    s"  ${urlEncodedForm.map((k, v) => s"${YELLOW}$k${BLUE} = ${CYAN}${v}${RESET}").mkString("\n  ")}"

  inline def maybeFormField(key: String): Option[String] =
    urlEncodedForm.get(key)

  inline def getFormField(key: String): String =
    urlEncodedForm
      .get(key)
      .getOrElse(
        s"Http form field '$key' not found, has:$formDebugString}"
      )

  inline def getFormField(key: String, defaultValue: String): String =
    urlEncodedForm.get(key).getOrElse(defaultValue)

  inline def maybeStageVariable(key: String): Option[String] =
    Option(stageVariables).flatMap(_.get(key))

  inline def maybeStageVariable(key: String, defaultValue: String): String =
    Option(stageVariables).flatMap(_.get(key)).getOrElse(defaultValue)

  final lazy val lambdaAlias: String =
    maybeStageVariable("LAMBDA_ALIAS").getOrElse("beta")

  final def computePathWithQueryParameters: String =
    modifyPath(path => s"https://foo${path}")
      .toHttpRequest()
      .uri
      .toString
      .drop(11)

  final def toHttpRequest(
      allowedHeaders: Set[String] = Set.empty,
      disallowedHeaders: Set[String] = Set.empty
  ): Request[String] =
    Uri.parse(this.path).match {

      case Right(validUri) if validUri.isAbsolute =>
        val allowedHeadersLowerCase = allowedHeaders.map(_.toLowerCase())
        val disallowedHeadersLowerCase = disallowedHeaders.map(_.toLowerCase())
        quickRequest
          .method(
            Method(Option(this.httpMethod).getOrElse("GET")),
            validUri
              .addParams(
                Option(this.queryStringParameters).getOrElse(Map.empty)
              )
          )
          .body(Option(body).getOrElse(""))
          .contentType(
            maybeHeader("Content-Type").getOrElse("plain/text")
          )
          .headers(
            Option(headers)
              .getOrElse(Map.empty)
              .filterNot((name, _) =>
                val n = name.toLowerCase
                !allowedHeadersLowerCase.contains(n)
                && (disallowedHeadersLowerCase.contains(n)
                  || n.startsWith("x-")
                  || n == "host"
                  || n == "user-agent"
                  || n == "content-type"
                  || n == "content-length"
                  || n == "content-encoding"
                  || n == "accept-encoding")
              )
          )

      case Right(relativeUri) =>
        throw new Exception(s"Expected absolute URI but got $relativeUri")

      case Left(error) =>
        throw new Exception(s"Error transforming to http request: $error")
    }

  /** Send request to the target host and return response wrapped in ApiGatewayResponse.
    */
  inline def passTo(
      host: String,
      debug: String => Unit
  ): Response[String] = {
    this
      .withPath(s"https://${host}${this.path}")
      .toHttpRequest()
      .printDebug(debug)
      .send()
      .printDebug(debug)
  }

  inline def passTo(
      host: String,
      headers: Map[String, String],
      debug: String => Unit
  ): Response[String] = {
    this
      .withPath(s"https://${host}${this.path}")
      .toHttpRequest()
      .headers(headers)
      .printDebug(debug)
      .send()
      .printDebug(debug)
  }

  inline def passTo(
      host: String,
      allowedHeaders: Set[String],
      disallowedHeaders: Set[String],
      customHeaders: Map[String, String],
      debug: String => Unit
  ): Response[String] = {
    this
      .withPath(s"https://${host}${this.path}")
      .toHttpRequest(allowedHeaders, disallowedHeaders)
      .headers(customHeaders)
      .printDebug(debug)
      .send()
      .printDebug(debug)
  }

}

object ApiGatewayRequest {

  inline def apply(
      httpMethod: String,
      path: String
  ): ApiGatewayRequest =
    ApiGatewayRequest(
      httpMethod = httpMethod,
      resource = path,
      path = path,
      body = null,
      isBase64Encoded = false,
      headers = null,
      stageVariables = null,
      queryStringParameters = null,
      requestContext = null
    )

  inline def apply(
      httpMethod: String,
      path: String,
      body: ujson.Value
  ): ApiGatewayRequest =
    ApiGatewayRequest(
      httpMethod = httpMethod,
      resource = path,
      path = path,
      body = write(body),
      isBase64Encoded = false,
      headers = Map("Content-Type" -> "application/json"),
      stageVariables = null,
      queryStringParameters = null,
      requestContext = null
    )

  inline def apply(
      httpMethod: String,
      path: String,
      body: String
  ): ApiGatewayRequest =
    ApiGatewayRequest(
      httpMethod = httpMethod,
      resource = path,
      path = path,
      body = body,
      isBase64Encoded = false,
      headers = null,
      stageVariables = null,
      queryStringParameters = null,
      requestContext = null
    )

  inline def apply(
      httpMethod: String,
      path: String,
      headers: Map[String, String]
  ): ApiGatewayRequest =
    ApiGatewayRequest(
      httpMethod = httpMethod,
      resource = path,
      path = path,
      body = null,
      isBase64Encoded = false,
      headers = headers,
      stageVariables = null,
      queryStringParameters = null,
      requestContext = null
    )

  inline def apply(
      httpMethod: String,
      path: String,
      body: String,
      headers: Map[String, String],
      stageVariables: Map[String, String]
  ): ApiGatewayRequest =
    ApiGatewayRequest(
      httpMethod = httpMethod,
      resource = path,
      path = path,
      body = body,
      isBase64Encoded = false,
      headers = headers,
      stageVariables = stageVariables,
      queryStringParameters = null,
      requestContext = null
    )

  given ReadWriter[ApiGatewayRequest] = {

    extension (obj: ujson.Obj)
      def readField[T: ReadWriter](key: String): T =
        obj.value
          .get(key)
          .map(_.readAs[T])
          .getOrElse(
            throw new ApiGatewayRequestParseException(
              s"Expected ApiGatewayRequest to have a field $key"
            )
          )

      def readFieldWithDefault[T: ReadWriter](key: String, default: T): T =
        obj.value
          .get(key)
          .map(_.readAs[T])
          .getOrElse(default)

      def readFieldOrNull[T: ReadWriter](key: String): T | Null =
        obj.value
          .get(key)
          .map(_.readAs[T])
          .getOrElse(null)

    readwriter[ujson.Value].bimap(
      request =>
        ujson.Obj.from(
          Seq(
            Some("httpMethod" -> ujson.Str(request.httpMethod)),
            Some("resource" -> ujson.Str(request.resource)),
            Some("path" -> ujson.Str(request.path)),
            Option(request.body).map(v => ("body" -> ujson.Str(v))),
            "isBase64Encoded" -> ujson.Bool(request.isBase64Encoded),
            Option(request.headers).map(v => ("headers" -> v.writeAsJson)),
            Option(request.stageVariables).map(v => ("stageVariables" -> v.writeAsJson)),
            Option(request.queryStringParameters).map(v => ("queryStringParameters" -> v.writeAsJson)),
            Option(request.requestContext).map(v => ("requestContext" -> v.writeAsJson)),
            Option(request.headers).map(v => ("headers" -> v.writeAsJson))
          ).collect { case Some(x) => x }
        ),
      {
        case obj: ujson.Obj =>
          ApiGatewayRequest(
            httpMethod = obj.readField[String]("httpMethod"),
            path = obj.readField[String]("path"),
            resource = obj.readFieldWithDefault[String](
              "resource",
              obj.readField[String]("path")
            ),
            body = obj.readFieldOrNull[String]("body"),
            isBase64Encoded = obj.readFieldWithDefault[Boolean]("isBase64Encoded", false),
            headers = obj.readFieldOrNull[Map[String, String]]("headers"),
            stageVariables = obj.readFieldOrNull[Map[String, String]]("stageVariables"),
            queryStringParameters = obj.readFieldOrNull[Map[String, String]]("queryStringParameters"),
            requestContext = obj.readFieldOrNull[Map[String, ujson.Value]]("requestContext")
          )
        case other =>
          throw new ApiGatewayRequestParseException(
            s"Expected ApiGatewayRequest to be a JSON object but got ${ujson.write(other)}"
          )
      }
    )
  }
}
