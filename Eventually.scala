package org.encalmo.lambda

import scala.io.AnsiColor
import scala.util.control.NonFatal

object Eventually {

  trait Patience {
    def newEpisode(): Patience.Episode
  }

  object Patience {

    trait Episode {
      def beforeStart: Unit
      def isExhausted: Boolean
      def failedAgain: Unit
      def success: Unit
      def totalWaitTime: String
    }

    def apply(
        maximumFailureCount: Int = 5,
        initialDelayMiliseconds: Int = 500,
        backoffFactor: Double = 2d,
        maximumTotalTimeSeconds: Int = 300,
        maximumDelaySeconds: Int = 60,
        secondBackoffFactor: Double = 1d,
        delayFirstInvocation: Boolean = false
    ): Patience = new Patience {

      override def newEpisode(): Episode = new Patience.Episode {
        val beginning = System.currentTimeMillis()
        val deadline: Long =
          System.currentTimeMillis() + (maximumTotalTimeSeconds * 1000)
        var failureCount = 0

        inline def isExhausted: Boolean = {
          failureCount >= maximumFailureCount
          || System.currentTimeMillis() >= deadline
        }

        def beforeStart: Unit = {
          if (delayFirstInvocation)
          then Thread.sleep(initialDelayMiliseconds)
        }

        def failedAgain: Unit = {
          failureCount = failureCount + 1
          if (!isExhausted) {
            val delayIncreaseFactor =
              if (secondBackoffFactor == 1d)
              then Math.pow(backoffFactor, (failureCount - 1))
              else
                Math.pow(
                  backoffFactor
                    * Math.pow(secondBackoffFactor, (failureCount - 1)),
                  (failureCount - 1)
                )
            val nextWaitMiliseconds = Math.max(
              Math.min(
                maximumDelaySeconds * 1000,
                Math.min(
                  (initialDelayMiliseconds * delayIncreaseFactor).toLong,
                  Math.abs(deadline - System.currentTimeMillis())
                )
              ),
              initialDelayMiliseconds
            )
            print(s"${AnsiColor.YELLOW_B}${AnsiColor.BLACK}")
            print(
              s"Failure $failureCount/$maximumFailureCount. Total wait time $totalWaitTime. Waiting another ${nextWaitMiliseconds} ms ... "
                .padTo(90, " ")
                .mkString
            )
            println(AnsiColor.RESET)
            Thread.sleep(nextWaitMiliseconds)
          }
        }

        inline def totalWaitTime: String = {
          val ms = System.currentTimeMillis() - beginning
          if ms < 1000 then s"$ms ms" else s"${ms / 1000} secs"
        }

        def success: Unit = {
          if (failureCount > 0) then
            print(s"${AnsiColor.GREEN_B}${AnsiColor.BLACK}")
            print(
              s"Eventually success! Took ${failureCount + 1} rounds and $totalWaitTime in total."
                .padTo(90, " ")
                .mkString
            )
            println(AnsiColor.RESET)
        }
      }
    }
  }

  given defaultPatience: Patience = Patience()

  inline def withPatience[T](patience: Patience)(body: Patience ?=> T): T = {
    body(using patience)
  }

  /** Try given assertions block until succeeds or patience exhausts. Optionally return the result.
    */
  def maybe[T](
      test: => T
  )(using patience: Patience): Option[T] = {
    val episode = patience.newEpisode()
    var result: Option[T] = None
    episode.beforeStart
    while (result.isEmpty && !episode.isExhausted) {
      try {
        result = Some(test)
        episode.success
      } catch {
        case NonFatal(e) =>
          episode.failedAgain
      }
    }
    result
  }

  /** Try given assertions block until succeeds or patience exhausts. Return the result or fail.
    */
  def eventually[T](
      test: => T
  )(using patience: Patience): T = {
    val episode = patience.newEpisode()
    var result: Option[Either[Throwable, T]] = None
    episode.beforeStart
    while ((result.isEmpty || result.exists(_.isLeft)) && !episode.isExhausted) {
      try {
        result = Some(Right(test))
      } catch {
        case NonFatal(e) =>
          result = Some(Left(e))
          episode.failedAgain
      }
    }
    result.get.fold(
      {
        case e: EventuallyKeepPolling =>
          throw new EventuallyPatienceExhausted(
            s"Eventually patience has been exhausted after ${episode.totalWaitTime}, polling has stopped."
          )
        case error => throw error
      },
      result => result
    )
  }

  class EventuallyPatienceExhausted(message: String) extends Exception(message, null, true, false)
  class EventuallyKeepPolling extends Exception(null, null, true, false)

  def keepPolling(): Nothing =
    throw new EventuallyKeepPolling()
}
