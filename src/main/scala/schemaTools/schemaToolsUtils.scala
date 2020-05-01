package schemaTools

import net.liftweb.json.DefaultFormats
import com.fasterxml.jackson.databind.ObjectMapper

import utils.AppConfiguration._
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper

import scala.io.Source
import scala.util.parsing.json.JSON

object schemaToolsUtils extends utils.loggerBase {

    def getSchemaName(collection :  String) : Option[String] = {
    try {
      val mapCollectionShema = JSON.parseFull(associatedCollectionSchema).get.asInstanceOf[Map[String,String]]
      val schemaName = mapCollectionShema.filterKeys(_ == collection).values.head
      Some(schemaName)

    } catch {
      case exception: Exception => {
        writeLog("error", s"Unabled to map collection name $collection to schema name")
        exception.printStackTrace()
        None
      }
    }
  }

  def getSchema(collection : String, extension : String, schemaPath : String) : Option[String] = {
    try {

      implicit val formats: DefaultFormats.type = DefaultFormats

      val schema = schemaToolsUtils.getSchemaName(collection)
      if (schema.isDefined) {
        val mapper = new ObjectMapper()
        val source = Source.fromFile(schemaPath + schema.get + "." + extension)
        val jsonValidationSchema = mapper.readTree(source.getLines().mkString)

        Some(jsonValidationSchema.toPrettyString)

      } else {
        writeLog("error", "Unabled to execute getSchema method because schema is not defined")
        None
      }
    } catch {
      case exception: Exception => {
        writeLog("error", "Unabled to execute getSchema method")
        exception.printStackTrace()
        None
      }
    }
  }

  def jsonToYaml(jsonString : String) : String = {
    val jsonNodeTree = new ObjectMapper().readTree(jsonString)
    val jsonAsYaml = new YAMLMapper().writeValueAsString(jsonNodeTree)
    jsonAsYaml
  }
}
