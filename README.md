<a href="https://github.com/encalmo/scala-aws-lambda-utils">![GitHub](https://img.shields.io/badge/github-%23121011.svg?style=for-the-badge&logo=github&logoColor=white)</a> <a href="https://central.sonatype.com/artifact/org.encalmo/scala-aws-lambda-utils_3" target="_blank">![Maven Central Version](https://img.shields.io/maven-central/v/org.encalmo/scala-aws-lambda-utils_3?style=for-the-badge)</a> <a href="https://encalmo.github.io/scala-aws-lambda-utils/scaladoc/org/encalmo/lambda.html" target="_blank"><img alt="Scaladoc" src="https://img.shields.io/badge/docs-scaladoc-red?style=for-the-badge"></a>

# scala-aws-lambda-utils

This Scala3 library provides models and utilities supplementing [`scala-aws-lambda-runtime`](https://github.com/encalmo/scala-aws-lambda-runtime).

## Table of contents

- [Motivation](#motivation)
- [Dependencies](#dependencies)
- [Usage](#usage)
- [Models](#models)
- [Exceptions](#exceptions)
- [Extensions](#extensions)
- [Patterns](#patterns)
- [Utils](#utils)
- [Project content](#project-content)

## Motivation

While [`scala-aws-lambda-runtime`](https://github.com/encalmo/scala-aws-lambda-runtime) provides a framework for writing AWS lambda using Scala, this library adds several opinionated models, exceptions, and extensions to facilitate common tasks using the uJson and sttp libraries.

## Dependencies

   - [Scala](https://www.scala-lang.org) >= 3.3.5
   - [Scala **toolkit** 0.7.0](https://github.com/scala/toolkit)
   - org.encalmo [**upickle-utils** 0.9.3](https://central.sonatype.com/artifact/org.encalmo/upickle-utils_3)

## Usage

Use with SBT

    libraryDependencies += "org.encalmo" %% "scala-aws-lambda-utils" % "0.9.5"

or with SCALA-CLI

    //> using dep org.encalmo::scala-aws-lambda-utils:0.9.5

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



## Project content

```
├── .github
│   └── workflows
│       ├── pages.yaml
│       ├── release.yaml
│       └── test.yaml
│
├── .gitignore
├── .scalafmt.conf
├── ApiGatewayExceptions.scala
├── ApiGatewayRequest.scala
├── ApiGatewayRequest.test.scala
├── ApiGatewayRequestBodyParseException.scala
├── ApiGatewayRequestParseException.scala
├── ApiGatewayResponse.scala
├── Attempt.scala
├── ConsoleUtils.scala
├── Error.scala
├── Error.test.scala
├── Eventually.scala
├── HasErrorCode.scala
├── LICENSE
├── OptionPickler.scala
├── project.scala
├── README.md
├── SqsEvent.scala
├── test.sh
├── Utils.scala
└── Utils.test.scala
```

