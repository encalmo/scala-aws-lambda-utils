package org.encalmo.lambda

final class ApiGatewayRequestParseException(message: String)
    extends Exception(message)
    with ApiGatewayBadRequestException
    with HasErrorCode("InvalidRequestError") {
  override def getMessage(): String = message
}
