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

  class MarshallingError(reason: String, source: String) extends Exception(s"$reason, input: ${source}")
  class UnmarshallingError(reason: String, source: String) extends Exception(s"$reason, input: ${source}")

  val rfcDateTime = DateTimeFormatter
    .ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)

  def rfcDateTimeNow() = rfcDateTime.format(ZonedDateTime.now())

  extension [T](value: Option[T])
    inline def writeAsJsonOrNull: ujson.Value =
      try {
        value
          .map(v =>
            inline scala.compiletime.erasedValue[T] match {
              case _: String  => ujson.Str(v.asInstanceOf[String])
              case _: Int     => ujson.Num(v.asInstanceOf[Int])
              case _: Long    => ujson.Num(v.asInstanceOf[Long].toDouble)
              case _: Double  => ujson.Num(v.asInstanceOf[Double])
              case _: Float   => ujson.Num(v.asInstanceOf[Float])
              case _: Boolean => ujson.Bool(v.asInstanceOf[Boolean])
              case _: T =>
                upickle.default.writeJs[T](v)(using scala.compiletime.summonInline[upickle.default.Writer[T]])
            }
          )
          .getOrElse(ujson.Null)
      } catch {
        case e => throw new MarshallingError(e.getMessage, value.toString)
      }

    inline def rightOrElse[L](alternative: => Either[L, T]): Either[L, T] =
      value match {
        case Some(value) => Right(value)
        case None        => alternative
      }

  extension [L: ReadWriter, R: ReadWriter](either: Either[L, R])
    inline def writeAsJson: ujson.Value = either.fold(writeJs, writeJs)
    inline def writeAsJsonObject: ujson.Obj =
      either.fold(writeJs, writeJs).asInstanceOf[ujson.Obj]
    inline def writeAsString: String =
      either.fold(l => ujson.write(writeJs(l)), r => ujson.write(writeJs(r)))

  extension [T: ReadWriter](entity: T)
    inline def writeAsJson: ujson.Value = writeJs(entity)
    inline def writeAsJsonObject: ujson.Obj =
      writeJs(entity).asInstanceOf[ujson.Obj]
    inline def writeAsString: String = ujson.write(writeJs(entity))

  extension (string: String) {
    inline def readAsJson: ujson.Value =
      Try(ujson.read(string)).getOrElse(ujson.Str(string))

    inline def maybeReadAsJson: Option[ujson.Value] =
      Try(ujson.read(string)).toOption

    inline def readAs[T: ReadWriter]: T =
      try (read(string))
      catch {
        case NonFatal(e) =>
          throw new UnmarshallingError(e.getMessage, string)
      }

    inline def maybeReadAs[T: ReadWriter]: Option[T] =
      try (Some(read(string)))
      catch {
        case NonFatal(e) => None
      }

    inline def readAsEither[L: ReadWriter, R: ReadWriter]: Either[L, R] =
      try (Right(read[R](string)))
      catch {
        case NonFatal(_) =>
          try (Left(read[L](string)))
          catch {
            case NonFatal(e) =>
              throw new UnmarshallingError(e.getMessage, string)
          }
      }

    inline def maybeReadAsEither[L: ReadWriter, R: ReadWriter]: Option[Either[L, R]] =
      try (Some(Right(read[R](string))))
      catch {
        case NonFatal(_) =>
          try (Some(Left(read[L](string))))
          catch {
            case NonFatal(e) => None
          }
      }
  }

  type TransformValue = PartialFunction[ujson.Value, ujson.Value]

  extension (value: ujson.Value) {
    inline def getString(p: String): String = value.apply(p).str
    inline def maybeString(p: String): Option[String] =
      value.objOpt
        .flatMap(_.get(p).flatMap(_.strOpt))
    inline def getInt(p: String): Int = value.apply(p).num.toInt
    inline def maybeInt(p: String): Option[Int] =
      value.objOpt
        .flatMap(_.get(p).flatMap(_.numOpt))
        .flatMap(n => Try(n.toInt).toOption)
    inline def getBoolean(p: String): Boolean = value.apply(p).bool
    inline def maybeBoolean(p: String): Option[Boolean] =
      value.objOpt
        .flatMap(_.get(p).flatMap(_.boolOpt))

    inline def readAs[T]: T =
      try {
        inline scala.compiletime.erasedValue[T] match {
          case _: String  => value.str.asInstanceOf[T]
          case _: Int     => value.num.toInt.asInstanceOf[T]
          case _: Long    => value.num.toLong.asInstanceOf[T]
          case _: Double  => value.num.asInstanceOf[T]
          case _: Float   => value.num.toFloat.asInstanceOf[T]
          case _: Boolean => value.bool.asInstanceOf[T]
          case _: T =>
            upickle.default.read[T](value)(using scala.compiletime.summonInline[upickle.default.Reader[T]])
        }
      } catch {
        case e => throw new UnmarshallingError(e.getMessage, value.toString)
      }

    inline def maybeReadAs[T]: Option[T] =
      inline scala.compiletime.erasedValue[T] match {
        case _: String  => value.strOpt.asInstanceOf[Option[T]]
        case _: Int     => value.numOpt.map(_.toInt).asInstanceOf[Option[T]]
        case _: Long    => value.numOpt.map(_.toLong).asInstanceOf[Option[T]]
        case _: Double  => value.numOpt.asInstanceOf[Option[T]]
        case _: Float   => value.numOpt.map(_.toFloat).asInstanceOf[Option[T]]
        case _: Boolean => value.boolOpt.asInstanceOf[Option[T]]
        case _: T =>
          try (Some(upickle.default.read[T](value)(using scala.compiletime.summonInline[upickle.default.Reader[T]])))
          catch {
            case e => None
          }
      }

    inline def readAsEither[L: ReadWriter, R: ReadWriter]: Either[L, R] =
      try (Right(upickle.default.read[R](value)))
      catch {
        case NonFatal(_) =>
          try (Left(upickle.default.read[L](value)))
          catch {
            case NonFatal(e) =>
              throw new UnmarshallingError(e.getMessage, value.toString)
          }
      }

    inline def maybeReadAsEither[L: ReadWriter, R: ReadWriter]: Option[Either[L, R]] =
      try (Some(Right(upickle.default.read[R](value))))
      catch {
        case NonFatal(_) =>
          try (Some(Left(upickle.default.read[L](value))))
          catch {
            case NonFatal(e) => None
          }
      }

    /** Retrieve some value at json path, or return None. */
    inline def get(path: String): Option[ujson.Value] =
      ValueUtils.getValue(ValueUtils.parsePath(path), value)

    /** Retrieve some value at json path, or return None. */
    inline def getByPath(path: String): Option[ujson.Value] =
      ValueUtils.getValue(ValueUtils.parsePath(path), value)

    /** Retrieve some entitty at json path, or return None. */
    inline def readByPath[T](path: String): Option[T] =
      ValueUtils
        .getValue(ValueUtils.parsePath(path), value)
        .flatMap(_.maybeReadAs[T])

    /** Set string value at json path, create missing path parts if needed. */
    inline def set(path: String, string: String): value.type =
      set(path, ujson.Str(string), force = true)
      value

    /** Set integer value at json path, create missing path parts if needed. */
    inline def set(path: String, number: Int): value.type =
      set(path, ujson.Num(number), force = true)
      value

    /** Set double value at json path, create missing path parts if needed. */
    inline def set(path: String, number: Double): value.type =
      set(path, ujson.Num(number), force = true)
      value

    /** Set boolean value at json path, create missing path parts if needed. */
    inline def set(path: String, boolean: Boolean): value.type =
      set(path, ujson.Bool(boolean), force = true)
      value

    inline def setByPath(
        path: String,
        valueToSet: ujson.Value,
        force: Boolean
    ): value.type =
      ValueUtils.setValue(ValueUtils.parsePath(path), value, valueToSet, force)
      value

    inline def writeByPath[T: ReadWriter](
        path: String,
        valueToSet: T,
        force: Boolean
    ): value.type =
      ValueUtils.setValue(
        ValueUtils.parsePath(path),
        value,
        valueToSet.writeAsJson,
        force
      )
      value

    /** Set value at json path. If force=true then create missing path parts. */
    inline def set(
        path: String,
        valueToSet: ujson.Value,
        force: Boolean
    ): value.type =
      ValueUtils.setValue(ValueUtils.parsePath(path), value, valueToSet, force)
      value

    /** Remove value at json path, if exists. */
    inline def remove(path: String): Unit =
      set(path, ujson.Null, force = false)

    /** Transform value at json path. If force=true then create missing path parts.
      */
    inline def transform(
        path: String,
        transform: TransformValue,
        force: Boolean
    ): value.type =
      ValueUtils.transformValue(
        ValueUtils.parsePath(path),
        value,
        transform,
        force
      )
      value

    @scala.annotation.targetName("plus")
    infix def +(field: (String, ujson.Value)): ujson.Obj =
      value match {
        case Obj(obj) => Obj(field, obj.value.toSeq*)
        case other =>
          throw new Exception(s"Cannot add a field to JSON element $other")
      }

    @scala.annotation.targetName("minus")
    infix def -(key: String): ujson.Obj =
      value match {
        case Obj(obj) =>
          Obj.from(obj.filterNot(_._1 == key))
        case other =>
          throw new Exception(s"Cannot add a field to JSON element $other")
      }

  }

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

object ValueUtils {

  inline def parsePath(path: String): Iterator[String] =
    path.split("[.\\[\\]]").filterNot(_.isEmpty).iterator

  /** Retrieve some value at json path, or return None. */
  def getValue(
      pathIterator: Iterator[String],
      target: ujson.Value
  ): Option[ujson.Value] =
    if (pathIterator.hasNext) then {
      val selector = pathIterator.next()
      if (selector.isEmpty) then getValue(pathIterator, target)
      else
        target match {
          case ujson.Obj(fields) =>
            fields.get(selector) match {
              case Some(value) => getValue(pathIterator, value)
              case None        => None
            }
          case ujson.Arr(array) =>
            if selector == "*" then
              val subpath = pathIterator.toSeq
              Some(
                ujson.Arr(
                  array
                    .map(value => getValue(subpath.iterator, value))
                    .collect { case Some(v) => v }
                )
              )
            else if selector.toIntOption.isDefined then
              val index = selector.toInt
              if (index >= 0 && index < array.length)
              then getValue(pathIterator, array(selector.toInt))
              else None
            else None
          case value =>
            if (pathIterator.hasNext)
            then getValue(pathIterator, value)
            else None

        }
    } else if (target == ujson.Null) then None
    else Some(target)

  /** Set value at json path. If force=true then add missing path parts.
    */
  def setValue(
      pathIterator: Iterator[String],
      target: ujson.Value,
      valueToSet: ujson.Value,
      force: Boolean
  ): Unit =
    if (pathIterator.hasNext) then {
      val selector = pathIterator.next()
      target match {
        case ujson.Obj(fields) =>
          setValueOnElement(
            fields.get(selector),
            selector,
            pathIterator,
            valueToSet,
            force,
            fields.update
          )
        case ujson.Arr(array) =>
          selector match {
            case s if s.toIntOption.isDefined =>
              val index = selector.toInt
              if (index >= 0)
              then
                if (index >= array.length)
                then
                  if force then
                    for (i <- array.length to index) array.append(ujson.Null)
                    setValueOnElement(
                      Some(array(index)),
                      selector,
                      pathIterator,
                      valueToSet,
                      force,
                      (s, v) => array.update(s.toInt, v)
                    )
                else
                  setValueOnElement(
                    Some(array(index)),
                    selector,
                    pathIterator,
                    valueToSet,
                    force,
                    (s, v) => array.update(s.toInt, v)
                  )
            case "*" =>
              array.zipWithIndex.foreach((value, index) =>
                setValueOnElement(
                  Some(value),
                  index.toString,
                  pathIterator,
                  valueToSet,
                  force,
                  (s, v) => array.update(s.toInt, v)
                )
              )
            case _ => ()
          }
        case _ => ()
      }
    } else ()

  private def setValueOnElement(
      element: Option[ujson.Value],
      selector: String,
      pathIterator: Iterator[String],
      valueToSet: ujson.Value,
      force: Boolean,
      update: (String, ujson.Value) => Unit
  ): Unit = element match {
    case Some(obj: ujson.Obj) =>
      if (pathIterator.hasNext)
      then setValue(pathIterator, obj, valueToSet, force)
      else update(selector, valueToSet)
    case Some(arr: ujson.Arr) =>
      if (pathIterator.hasNext)
      then setValue(pathIterator, arr, valueToSet, force)
      else update(selector, valueToSet)
    case Some(other) =>
      if (pathIterator.hasNext)
      then
        if (force) then
          val obj = ujson.Obj(selector -> ujson.Null)
          update(selector, obj)
          setValue(pathIterator, obj, valueToSet, force)
      else update(selector, valueToSet)
    case None =>
      if (force) then
        if (pathIterator.hasNext)
        then
          val obj = ujson.Obj(selector -> ujson.Null)
          update(selector, obj)
          setValue(pathIterator, obj, valueToSet, force)
        else update(selector, valueToSet)
  }

  /** Transform value at json path. If force=true then add missing path parts.
    */
  def transformValue(
      pathIterator: Iterator[String],
      target: ujson.Value,
      transform: PartialFunction[ujson.Value, ujson.Value],
      force: Boolean
  ): Unit =
    if (pathIterator.hasNext) then {
      val selector = pathIterator.next()
      target match {
        case ujson.Obj(fields) =>
          transformValueOnElement(
            fields.get(selector),
            selector,
            pathIterator,
            transform,
            force,
            fields.update
          )
        case ujson.Arr(array) =>
          selector match {
            case s if s.toIntOption.isDefined =>
              val index = selector.toInt
              if (index >= 0)
              then
                if (index >= array.length)
                then
                  if force then
                    for (i <- array.length to index) array.append(ujson.Null)
                    transformValueOnElement(
                      Some(array(index)),
                      selector,
                      pathIterator,
                      transform,
                      force,
                      (s, v) => array.update(s.toInt, v)
                    )
                else
                  transformValueOnElement(
                    Some(array(index)),
                    selector,
                    pathIterator,
                    transform,
                    force,
                    (s, v) => array.update(s.toInt, v)
                  )
            case "*" =>
              array.zipWithIndex.foreach((value, index) =>
                transformValueOnElement(
                  Some(value),
                  index.toString,
                  pathIterator,
                  transform,
                  force,
                  (s, v) => array.update(s.toInt, v)
                )
              )
            case _ => ()
          }
        case _ => ()
      }
    } else ()

  private def transformValueOnElement(
      element: Option[ujson.Value],
      selector: String,
      pathIterator: Iterator[String],
      transform: PartialFunction[ujson.Value, ujson.Value],
      force: Boolean,
      update: (String, ujson.Value) => Unit
  ): Unit = element match {
    case Some(obj: ujson.Obj) =>
      if (pathIterator.hasNext)
      then transformValue(pathIterator, obj, transform, force)
      else update(selector, transform.applyOrElse(obj, _ => obj))
    case Some(arr: ujson.Arr) =>
      if (pathIterator.hasNext)
      then transformValue(pathIterator, arr, transform, force)
      else update(selector, transform.applyOrElse(arr, _ => arr))
    case Some(other) =>
      if (pathIterator.hasNext)
      then
        if (force) then
          val obj = ujson.Obj(selector -> ujson.Null)
          update(selector, obj)
          transformValue(pathIterator, obj, transform, force)
      else update(selector, transform.applyOrElse(other, _ => other))
    case None =>
      if (force) then
        if (pathIterator.hasNext)
        then
          val obj = ujson.Obj(selector -> ujson.Null)
          update(selector, obj)
          transformValue(pathIterator, obj, transform, force)
        else if (transform.isDefinedAt(ujson.Null))
        then update(selector, transform(ujson.Null))
  }

}
