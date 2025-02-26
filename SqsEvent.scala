package org.encalmo.lambda

import org.encalmo.lambda.Utils.*
import upickle.default.*

final case class SqsEvent(
    Records: Seq[SqsEvent.Record]
) derives ReadWriter

object SqsEvent {

  final case class Record(
      messageId: String,
      body: String,
      attributes: Map[String, String],
      eventSource: String,
      eventSourceARN: String
  ) derives ReadWriter {

    final def maybeParseBodyAs[T: ReadWriter]: Option[T] =
      try (Some(body.readAs[T]))
      catch { case e => None }

    final def maybeParseBodyAsJson: Option[ujson.Value] =
      try (Some(ujson.read(body)))
      catch { case e => None }
  }
}
