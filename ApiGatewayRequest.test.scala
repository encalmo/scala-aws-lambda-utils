package org.encalmo.lambda

class ApiGatewayRequestSpec extends munit.FunSuite {

  test("computePathWithQueryParameters") {
    val request = ApiGatewayRequest(
      httpMethod = "POST",
      resource = "/foo",
      path = "/beta/foo",
      body = null,
      isBase64Encoded = false,
      headers = null,
      stageVariables = null,
      queryStringParameters = Map("bar" -> "123", "zaz" -> "zo&"),
      requestContext = null
    )

    val path = request.computePathWithQueryParameters
    assertEquals(path, "/beta/foo?bar=123&zaz=zo%26")

    val serialized = upickle.default.write(request)
    assertEquals(
      serialized,
      """{"httpMethod":"POST","resource":"/foo","path":"/beta/foo","queryStringParameters":{"bar":"123","zaz":"zo&"}}"""
    )
  }

  test("replacePathWithResourceIfNotProxy - not proxy") {
    val request = ApiGatewayRequest(
      httpMethod = "POST",
      resource = "/foo",
      path = "/beta/foo",
      body = "{\"walletMethod\":\"gcash\",\"token\":\"0f4c1d38-38fc-4ff2-82f1-28dbe2023fc9\"}",
      isBase64Encoded = false,
      headers = null,
      stageVariables = null,
      queryStringParameters = null,
      requestContext = null
    )
    val path = request.replacePathWithResourceIfNotProxy.path
    assertEquals(path, "/foo")

    val serialized = upickle.default.write(request)
    assertEquals(
      serialized,
      """{"httpMethod":"POST","resource":"/foo","path":"/beta/foo","body":"{\"walletMethod\":\"gcash\",\"token\":\"0f4c1d38-38fc-4ff2-82f1-28dbe2023fc9\"}"}"""
    )
  }

  test("replacePathWithResourceIfNotProxy - proxy") {
    val request = ApiGatewayRequest(
      httpMethod = "POST",
      resource = "/{proxy+}",
      path = "/beta/foo",
      body = "foo",
      isBase64Encoded = true,
      headers = Map("Content-Type" -> "plain/text", "Accept" -> "plain/text"),
      stageVariables = Map("FOO" -> "BAR", "ZOO" -> "ZAZ"),
      queryStringParameters = Map("param1" -> "value1", "param2" -> "value2"),
      requestContext = Map(
        "contextParam1" -> "contextValue1",
        "contextParam2" -> "contextValue2"
      )
    )
    val path = request.replacePathWithResourceIfNotProxy.path
    assertEquals(path, "/beta/foo")

    val serialized = upickle.default.write(request)
    assertEquals(
      serialized,
      """{"httpMethod":"POST","resource":"/{proxy+}","path":"/beta/foo","body":"foo","headers":{"Content-Type":"plain/text","Accept":"plain/text"},"stageVariables":{"FOO":"BAR","ZOO":"ZAZ"},"queryStringParameters":{"param1":"value1","param2":"value2"},"requestContext":{"contextParam1":"contextValue1","contextParam2":"contextValue2"}}"""
    )
  }

  test("de-serialize api gateway requests") {
    val request = upickle.default.read[ApiGatewayRequest](
      """|{
         |    "path": "/payments/currency-exchange-rate",
         |    "httpMethod": "POST",
         |    "body": "{\"walletMethod\":\"gcash\",\"token\":\"0f4c1d38-38fc-4ff2-82f1-28dbe2023fc9\"}"
         |}""".stripMargin
    )
    assertEquals(request.httpMethod, "POST")
    assertEquals(request.path, "/payments/currency-exchange-rate")
    assertEquals(request.resource, "/payments/currency-exchange-rate")
    assertEquals(
      request.body,
      "{\"walletMethod\":\"gcash\",\"token\":\"0f4c1d38-38fc-4ff2-82f1-28dbe2023fc9\"}"
    )
  }

}
