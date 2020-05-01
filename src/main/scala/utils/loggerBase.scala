package utils

import java.util.Calendar

import com.typesafe.scalalogging.Logger

trait loggerBase {

  val logger = Logger(this.getClass)

  def writeLog(level : String, message : String) : Unit = level match {

    case "info" => {
      val exceptionTime = Calendar.getInstance.getTime()
      println(s"$exceptionTime : INFO : $message")
      logger.info(s"INFO : $message")}

    case "warn" => {
      val exceptionTime = Calendar.getInstance.getTime()
      println(s"$exceptionTime : WARN : $message")
      logger.warn(s" WARN : $message")}

    case "error" => {
      val exceptionTime = Calendar.getInstance.getTime()
      println(s"$exceptionTime : ERROR : $message")

      logger.error(
      s"""
         |
         |    ERROR : $message
         |
      """.stripMargin)}
  }
}