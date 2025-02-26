package org.encalmo.lambda

final class ApiGatewayRequestBodyParseException(e: Exception)
    extends Exception(e)
    with ApiGatewayBadRequestException
    with HasErrorCode("InvalidRequestError") {
  override def getMessage(): String = "Invalid payload."
}
