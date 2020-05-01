package utils

import java.io.{File, FileInputStream}
import java.util

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper

object AppConfiguration {

  private var props : java.util.HashMap[String,String] = new util.HashMap
  var interface = ""
  var port = ""
  var portHttps = ""
  var mongo_uri = ""
  var secret = ""
  var interfaceSecret  = ""
  var jsonSchemaPath = ""
  var associatedCollectionSchema = ""
  var associatedCollectionDatabase = ""

  def initializeAppConfig(configFilePath : String) : Unit = {

    val fileInputStream = new FileInputStream(new File(configFilePath))
    val confObj = new YAMLMapper().readTree(fileInputStream)

    interface = confObj.get("interface").textValue()
    port = confObj.get("port").textValue()
    portHttps = confObj.get("portHttps").textValue()
    mongo_uri = confObj.get("mongo_uri").textValue()
    jsonSchemaPath = confObj.get("jsonSchemaPath").textValue()
    interfaceSecret = confObj.get("interfaceSecret").textValue()
    secret = confObj.get("secret").textValue()
    associatedCollectionSchema = confObj.get("associatedCollectionSchema").textValue()
    associatedCollectionDatabase = confObj.get("associatedCollectionDatabase").textValue()

  }

}
