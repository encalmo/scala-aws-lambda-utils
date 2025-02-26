package org.encalmo.lambda

object Attempt {

  extension [T, R](value: T)
    def attempt(test: T => R): Either[T, R] =
      try (Right(test(value)))
      catch {
        case e => Left(value)
      }

  extension [T, R1, R2](result: Either[T, R1])
    def attempt(test: T => R2): Either[T, R1 | R2] =
      result match {
        case Right(r) => Right(r)
        case Left(value) =>
          try (Right(test(value)))
          catch {
            case e => Left(value)
          }
      }

  extension [T, R](result: Either[T, R]) def union: T | R = result.fold(identity, identity)
}
