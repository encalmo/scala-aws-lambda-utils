package org.encalmo.lambda

class UtilsSpec extends munit.FunSuite {

  import Utils.*

  test("forget") {
    assertEquals(
      Right[Error, String]("Hello").forget,
      Right(())
    )
  }

  test("Option.tap") {
    var x = 0
    Some(1).tapSome(i => x = i)
    assertEquals(x, 1)
    Some(1).tapNone { x = 2 }
    assertEquals(x, 1)
    None.tapSome(i => x = 3)
    assertEquals(x, 1)
    None.tapNone { x = 4 }
    assertEquals(x, 4)
    Some(6).tap {
      case None    => x = 5
      case Some(i) => x = 2 * i
    }
    assertEquals(x, 12)
    None.tap {
      case None    => x = 7
      case Some(_) => x = 0
    }
    assertEquals(x, 7)
  }

  test("flatMapEO") {
    assertEquals(
      Right[Error, Option[String]](Some("Hello")).flatMapEO(_ => Right(Some("World"))),
      Right(Some("World"))
    )
    assertEquals(
      Right[Error, Option[String]](None).flatMapEO(_ => Right(Some("World"))),
      Right(None)
    )
    assertEquals(
      Left[Error, Option[String]](Error("Oops")).flatMapEO(_ => Right(Some("World"))),
      Left(Error("Oops"))
    )
  }

}
