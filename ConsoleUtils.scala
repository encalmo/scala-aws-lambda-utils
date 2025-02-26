package org.encalmo.lambda

import scala.io.AnsiColor

object ConsoleUtils {

  def printlnMessageBoxed(
      size: Int = 90,
      margin: Int = 2,
      color: String,
      frame: Char,
      message: String
  ): Unit =
    print(color)
    println(s"$frame" * size)
    printlnMessage(size, margin, message)
    println(s"$frame" * size)
    print(AnsiColor.RESET)

  def printlnMessageOverlined(
      size: Int = 90,
      margin: Int = 2,
      color: String,
      frame: Char,
      message: String
  ): Unit =
    print(color)
    println(s"$frame" * size)
    printlnMessage(size, margin, message)
    print(AnsiColor.RESET)

  def printlnMessageUnderlined(
      size: Int = 90,
      margin: Int = 2,
      color: String,
      frame: Char,
      message: String
  ): Unit =
    print(color)
    printlnMessage(size, margin, message)
    println(s"$frame" * size)
    print(AnsiColor.RESET)

  def printlnMessage(size: Int, margin: Int, message: String): Unit =
    print(" " * margin)
    val m1 = message.take(size - 2 * margin)
    val m2 =
      if (message.length() > m1.length())
      then
        val i = m1.lastIndexOf(" ")
        if (i > (size / 2) + (2 * margin))
        then
          if (m1(i - 1) == ',')
          then m1.substring(0, i)
          else m1.substring(0, i + 1)
        else m1
      else m1
    print(m2)
    println(" " * margin)
    if (message.length() > m2.length())
    then printlnMessage(size, margin, message.drop(m2.length()))
    else ()

}
