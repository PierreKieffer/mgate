package relational

import org.mongodb.scala.{Completed, MongoClient, MongoDatabase}
import org.mongodb.scala.bson.BsonObjectId
import org.mongodb.scala.{Completed, MongoClient}
import org.mongodb.scala.model.Filters._

import scala.concurrent.duration._
import org.mongodb.scala.result.UpdateResult
import schemaTools.schemaValidator._

import scala.concurrent.Future
import scala.concurrent.Await
import scala.util.{Failure, Success}

object relationalMethods extends utils.AkkaActor with utils.loggerBase  {

  def mongoInspector(_id : String, collectionName : String, mongoClientHubstairs: MongoClient, mongoClientItems
  : MongoClient)
  : Option[Boolean] = {
    try {

      val databaseName = handler.payloadParser.getDatabaseName(collectionName)
      val database: MongoDatabase = mongoClientHubstairs.getDatabase(databaseName)
      val collection = database.getCollection(collectionName)

      // check if exist
      val inspectorSize =
        collection
        .find(equal("_id", BsonObjectId(_id)))
        .map(doc => doc.size).head()

      val extractsize = Await.result(inspectorSize, 16.seconds)

      val idExist = extractsize match {
        case 0 => false
        case _ => true
      }
      Some(idExist)
    } catch {
      case exception: Exception => {
        writeLog("error", "Unable to launch mongoInspector")
        None
      }
    }
  }


}
