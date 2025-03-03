# scala-aws-lambda-utils

This Scala3 library provides models and utilities supplementing [`scala-aws-lambda-runtime`](https://github.com/encalmo/scala-aws-lambda-runtime).

## Motivation

While [`scala-aws-lambda-runtime`](https://github.com/encalmo/scala-aws-lambda-runtime) provides a framework for writing AWS lambda using Scala, this library adds several opinionated models, exceptions, and extensions to facilitate common tasks using the uJson and sttp libraries.

## Dependencies

- Scala 3.3.5
- ujson
- sttp4

## Usage

Use with SBT

    libraryDependencies += "org.encalmo" %% "scala-aws-lambda-utils" % "0.9.2"

or with SCALA-CLI

    //> using dep org.encalmo::scala-aws-lambda-utils:0.9.2

## Models

Common AWS Lambda request/response structures

- ApiGatewayRequest
- ApiGatewayResponse
- SqsEvent
- Error

## Exceptions

- ApiGatewayException
- ApiGatewayBadRequestException
- ApiGatewayUnauthorizedException
- ApiGatewayForbiddenException
- ApiGatewayNotFoundException
- ApiGatewayRequestParseException
- ApiGatewayRequestBodyParseException

## Extensions

- Utils

## Patterns

- Eventually
- Attempt

## Utils

- ConsoleUtils
- OptionPickler

