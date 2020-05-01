package schemaTools


import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.{JsonSchemaFactory, JsonValidator}

import scala.io.Source

object schemaValidator extends utils.loggerBase {

  case class schemaValidationResult (
                                    status : Boolean,
                                    report : String
                                    )

  def schemaValidation(collection : String, extension : String ,schemaPath : String, jsonData : String)
  : Option[schemaValidationResult]
  = {
    try {

//      val schema = collection.dropRight(1) // remove "s" from collection's name

      val schema = schemaToolsUtils.getSchemaName(collection)
      if (schema.isDefined){
        val mapper = new ObjectMapper()
        val source = Source.fromFile(schemaPath + schema.get + "." +extension)
        val jsonValidationSchema = mapper.readTree(source.getLines().mkString)
        val inputJson = mapper.readTree(jsonData)

        val factory: JsonSchemaFactory = JsonSchemaFactory.byDefault()
        val validator: JsonValidator = factory.getValidator
        val report: ProcessingReport = validator.validate(jsonValidationSchema, inputJson)
        source.close()
        Some(schemaValidationResult(report.isSuccess,report.toString))
      } else {
        writeLog("error", s"""Unabled to launch schemaValidation
                             |Unable to map collection name to schema name
                             |""".stripMargin)
        None
      }
    } catch {
      case exception: Exception => {
        writeLog("error", "Unabled to launch schemaValidation")
        exception.printStackTrace()
        None
      }
    }

  }

}
