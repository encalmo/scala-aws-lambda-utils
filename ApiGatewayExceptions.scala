package org.encalmo.lambda

trait ApiGatewayException {
  val statusCode: Int
}

trait ApiGatewayBadRequestException extends ApiGatewayException {
  final val statusCode: Int = 400
}

trait ApiGatewayUnauthorizedException extends ApiGatewayException {
  final val statusCode: Int = 401
}

trait ApiGatewayForbiddenException extends ApiGatewayException {
  final val statusCode: Int = 403
}

trait ApiGatewayNotFoundException extends ApiGatewayException {
  final val statusCode: Int = 404
}
