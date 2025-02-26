package org.encalmo.lambda

import upickle.default.*
import Utils.*
import java.time.Instant

case class Error(
    errorCode: String,
    errorMessage: String,
    additionalFields: Map[String, ujson.Value] = Map.empty
) {
  inline def success: Boolean = false

  inline def maybe(key: String): Option[ujson.Value] =
    additionalFields.get(key)

  override def equals(other: Any): Boolean =
    other match {
      case Error(errorCode, errorMessage, additionalFields) =>
        errorCode == errorCode
        && additionalFields == additionalFields

      case _ => false
    }
}

object Error {

  inline def apply(errorCode: String, errorMessage: String): Error =
    new Error(errorCode, errorMessage)

  inline def apply(errorCode: String): Error =
    new Error(errorCode, "")

  inline def apply(
      errorCode: String,
      errorMessage: String,
      additionalFields: (String, ujson.Value)*
  ): Error =
    new Error(
      errorCode,
      errorMessage,
      additionalFields = additionalFields.toMap
    )

  inline def from(throwable: Throwable): Error =
    new Error(getErrorCode(throwable), throwable.getMessage())

  given ReadWriter[Error] =
    readwriter[ujson.Obj].bimap(
      { error =>
        val explicitErrorFieldValue = error.additionalFields.find(_._1 == "error").map(_._2)
        val fields = Map(
          "success" -> ujson.Bool(false),
          "timestamp" -> ujson.Num(Instant.now().getEpochSecond().toDouble),
          "error" -> explicitErrorFieldValue.getOrElse(ujson.Str(error.errorCode)),
          "errorMessage" -> ujson.Str(error.errorMessage)
        ) ++ error.additionalFields
          ++ (if explicitErrorFieldValue.isDefined
              then Map(("errorCode", ujson.Str(error.errorCode)))
              else Map.empty)

        ujson.Obj.from(fields)
      },
      { obj =>
        val hasBothFields =
          obj.get("error").isDefined && obj.get("errorCode").isDefined
        val (errorCode, additionalFields) =
          if (hasBothFields)
          then
            (
              obj.maybeString("errorCode"),
              obj.value.toMap
                .removed("success")
                .removed("errorMessage")
                .removed("errorCode")
                .removed("timestamp")
                .toMap
            )
          else
            (
              obj
                .maybeString("errorCode")
                .orElse(
                  obj.maybeString("error")
                ),
              obj.value.toMap
                .removed("success")
                .removed("errorMessage")
                .removed("error")
                .removed("errorCode")
                .removed("timestamp")
            )
        errorCode
          .map(errorCode =>
            new Error(
              errorCode = errorCode,
              errorMessage = obj
                .maybeString("errorMessage")
                .orElse(obj.maybeString("message"))
                .getOrElse(""),
              additionalFields = additionalFields.filterNot(_._2 == ujson.Null)
            )
          )
          .getOrElse(
            throw new UnmarshallingError("Cannot construct Error", write(obj))
          )
      }
    )

  inline def getErrorCode(throwable: Throwable): String =
    throwable match {
      case hec: HasErrorCode => hec.errorCode
      case _ =>
        val code = throwable.getClass.getSimpleName()
        if (code.isBlank()) throwable.getClass.getTypeName()
        else code
    }
}
