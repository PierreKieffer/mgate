package handler

import java.util.Calendar

import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization.write
import org.mongodb.scala.bson.collection.mutable.Document
import utils.AppConfiguration._

import scala.util.parsing.json.JSON

object payloadParser extends utils.loggerBase {

  case class  insertPayload(
                               database: String,
                               collection : String,
                               document : Map[String,Any]
                             )

  case class upsertPayload (
                             database : String,
                             collection : String,
                             _id : String,
                             document : Map[String,Any]
                           )

  case class updateFieldPayload (
                                  database: String,
                                  collection : String,
                                  _id : String,
                                  document : Map[String,Any]
                                )

  case class deletePayload(
                          database: String,
                          collection: String,
                          _id : String
                          )

  def serializePayload(payload : String) : Document = {
    Document.apply(payload)
  }

  def insertPayloadParser(inputPostPayload : String, crudOperation : String) : Option[insertPayload]
  = {
    try  {
      implicit val formats: DefaultFormats.type = DefaultFormats
      val parsedPayload = crudOperation match {
        case "insert" =>  {
          val parsedResponse = JSON.parseFull(inputPostPayload).map{
            case jsonElem : Map [String,Any] => {
              insertPayload(
                getDatabaseName(jsonElem("collection").toString),
                jsonElem("collection").toString,
                jsonElem("document").asInstanceOf[Map[String,Any]]
              )
            }
          }.get
          Some(parsedResponse)
        }
      }
      parsedPayload

    } catch {
      case exception: Exception => {
        writeLog("error", "Error during insertPayloadParser")
        exception.printStackTrace()
        None
      }
    }
  }


  def updatePayloadParser(inputPostPayload : String, crudOperation : String) : Option[updateFieldPayload]
  = {
    try  {
      implicit val formats: DefaultFormats.type = DefaultFormats
      val parsedPayload = crudOperation match {
        case "updateField" => {
          val parsedResponse = JSON.parseFull(inputPostPayload).map{
            case jsonElem : Map [String,Any] => {
              updateFieldPayload(
                getDatabaseName(jsonElem("collection").toString),
                jsonElem("collection").toString,
                jsonElem("_id").toString,
                jsonElem("document").asInstanceOf[Map[String,Any]]
              )
            }
          }.get
          Some(parsedResponse)
        }
      }
      parsedPayload
    } catch {
      case exception: Exception => {
        writeLog("error", "Error during updatePayloadParser")
        exception.printStackTrace()
        None
      }
    }
  }

  def upsertPayloadParser(inputPostPayload : String, crudOperation : String) : Option[upsertPayload]
  = {
    try  {
      implicit val formats: DefaultFormats.type = DefaultFormats
      val parsedPayload = crudOperation match {
        case "upsert" => {
          val parsedResponse = JSON.parseFull(inputPostPayload).map{
            case jsonElem : Map [String,Any] => {
              upsertPayload(
                getDatabaseName(jsonElem("collection").toString),
                jsonElem("collection").toString,
                jsonElem("_id").toString,
                jsonElem("document").asInstanceOf[Map[String,Any]]
              )
            }
          }.get
          Some(parsedResponse)
        }
      }
      parsedPayload
    } catch {
      case exception: Exception => {
        writeLog("error", "Error during upsertPayloadParser")
        exception.printStackTrace()
        None
      }
    }
  }


  def deletePayloadParser(inputPostPayload : String, crudOperation : String) : Option[deletePayload]
  = {
    try  {
      implicit val formats: DefaultFormats.type = DefaultFormats
      val parsedPayload = crudOperation match {
        case "delete" => {
          val parsedResponse = JSON.parseFull(inputPostPayload).map{
            case jsonElem : Map [String,Any] => {
              deletePayload(
                getDatabaseName(jsonElem("collection").toString),
                jsonElem("collection").toString,
                jsonElem("_id").toString
              )
            }
          }.get
          Some(parsedResponse)
        }
      }
      parsedPayload
    } catch {
      case exception: Exception => {
        writeLog("error", "Error during deletePayloadParser")
        exception.printStackTrace()
        None
      }
    }
  }

  def getDatabaseName(collection : String) : String = {
    try  {
      val mapCollectionDatabase = JSON.parseFull(associatedCollectionDatabase).get.asInstanceOf[Map[String,String]]
      val databaseName = mapCollectionDatabase.filterKeys(_ == collection).values.head
      databaseName
  } catch {
      case e : Exception => {
        writeLog("error" , s"Unabled to exec getDatabaseName method for $collection ")
        println(e)
        "unknown"
      }
  }

  }
}

