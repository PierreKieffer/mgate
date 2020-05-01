package admin

import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization.write

import scala.util.parsing.json.JSON

import utils.AppConfiguration._

object schemaUpdate extends utils.loggerBase {

  /*
  payload :
  {"collection" : String,
  "schema" : {}}
   */

  case class updateSchemaPayload(
                                collection : String,
                                schema : String
                                )

  def parsePayload(inputPostPayload : String) : Option[updateSchemaPayload] = {
    try  {
      implicit val formats: DefaultFormats.type = DefaultFormats
      val parsedPayload = JSON.parseFull(inputPostPayload).map{
            case jsonElem : Map [String,Any] => {
              updateSchemaPayload(
                jsonElem("collection").toString,
                write(jsonElem("schema").asInstanceOf[Map[String,Any]])
              )
            }
          }.get
      Some(parsedPayload)

    } catch {
      case exception: Exception => {
        writeLog("error", "Error during payload parser")
        exception.printStackTrace()
        None
      }
    }
  }

  def deploySchema(updateSchemaPayload : updateSchemaPayload) : Unit = {
    try {
      val schemaName = schemaTools.schemaToolsUtils.getSchemaName(updateSchemaPayload.collection)
      val schema = updateSchemaPayload.schema

      adminUtils.fileWriter(schema,jsonSchemaPath,schemaName.get,"json")

    } catch {
      case exception: Exception => {
        writeLog("error", "Error during schema deployment")
        exception.printStackTrace()
      }
    }

  }

}
