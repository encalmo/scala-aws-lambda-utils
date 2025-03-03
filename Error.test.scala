package org.encalmo.lambda

import org.encalmo.utils.JsonUtils.*

class ErrorSpec extends munit.FunSuite {

  test("Marshall and unmarshall Error entity 1") {
    val enitity = Error("TestError")
    val marshalled = enitity.writeAsString
    println(marshalled)
    val unmarshalled = marshalled.readAs[Error]
    assertEquals(unmarshalled.errorCode, "TestError")
    assertEquals(unmarshalled.errorMessage, "")
    assertEquals(enitity, unmarshalled)
  }

  test("Marshall and unmarshall Error entity 2") {
    val enitity =
      Error(errorCode = "TestError", errorMessage = "Some test error message")
    val marshalled = enitity.writeAsString
    println(marshalled)
    val unmarshalled = marshalled.readAs[Error]
    assertEquals(unmarshalled.errorCode, "TestError")
    assertEquals(unmarshalled.errorMessage, "Some test error message")
    assertEquals(enitity, unmarshalled)
  }

  test("Marshall and unmarshall Error entity 3") {
    val enitity =
      Error(errorCode = "TestError", errorMessage = "Some test error message", "error" -> ujson.Str("Foo"))
    val marshalled = enitity.writeAsString
    println(marshalled)
    val unmarshalled = marshalled.readAs[Error]
    assertEquals(unmarshalled.errorCode, "TestError")
    assertEquals(unmarshalled.errorMessage, "Some test error message")
    assertEquals(unmarshalled.additionalFields("error"), ujson.Str("Foo"))
    assertEquals(enitity, unmarshalled)
  }

  test("Marshall and unmarshall Error entity 2") {
    val enitity =
      Error(
        errorCode = "TestError",
        errorMessage = "Some test error message",
        "foo" -> ujson.Str("bar")
      )
    val marshalled = enitity.writeAsString
    println(marshalled)
    val unmarshalled = marshalled.readAs[Error]
    assertEquals(enitity, unmarshalled)
    assertEquals(unmarshalled.errorCode, "TestError")
    assertEquals(unmarshalled.errorMessage, "Some test error message")
    assertEquals(unmarshalled.maybe("foo"), Some(ujson.Str("bar")))
  }

  test("Convert Throwable to Error") {
    assertEquals(
      Error.from(new IllegalStateException("some reason")),
      Error("IllegalStateException", "some reason")
    )
  }

}
