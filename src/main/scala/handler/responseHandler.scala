package handler

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol



object responseHandler extends utils.loggerBase {
  case class httpResponse(
                           statusCode : Int,
                           statusLog : String,
                           _id : String,
                           collection : String
                         )


  case object httpResponse extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val httpResponseFormat = jsonFormat4(httpResponse.apply)
  }

  def buildResponse(_id : String, collection : String , statusCode : Int, statusLog : String)
  : Option[httpResponse]= {
    try {
      Some(
        httpResponse(
          statusCode,
          statusLog,
          _id,
          collection
        )
      )
    } catch {
      case exception: Exception =>
      {
        writeLog("error", "Unable to exec buildResponse method")
        exception.printStackTrace()
        None
      }
    }
  }

}


