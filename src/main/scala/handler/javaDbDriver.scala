package handler

import java.util.Date

import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization.write
import com.mongodb.{BasicDBObject, MongoClient, MongoClientURI}
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import utils.AppConfiguration._
import org.bson.Document
import org.mongodb.scala.bson.collection.mutable.Document

import scala.concurrent.Future

object javaDbDriver extends utils.loggerBase {
  def insert(inputDocument : Map[String,Any], databaseName : String, collectionName : String)
  : Option[String] = {
    try {

      val mongoClient = new MongoClient(new MongoClientURI(mongo_uri))

      implicit val formats: DefaultFormats.type = DefaultFormats

      val createdAt = new Date()
      val createdAtUnix = System.currentTimeMillis()

      val insertJson = write( inputDocument ++ Map("createdAtUnix" -> createdAtUnix))
      val doc = org.bson.Document.parse(insertJson)

      val database: MongoDatabase = mongoClient.getDatabase(databaseName)
      val collection : MongoCollection[org.bson.Document] = database.getCollection(collectionName)
      collection.insertOne(doc)

      mongoClient.close()
      Some(doc.get("_id").toString)

    } catch {
      case exception: Exception => {
        writeLog("error","Unable to exec javaDbDriver.insert method")
        exception.printStackTrace()
        None
      }

    }
  }
}
