package org.encalmo.lambda

import upickle.default.*

final case class ApiGatewayResponse(
    body: String,
    statusCode: Int,
    headers: Map[String, String],
    isBase64Encoded: Boolean
) derives ReadWriter
