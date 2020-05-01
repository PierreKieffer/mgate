package handler

import java.util.{Calendar, Date}

import org.mongodb.scala.{Completed, MongoClient, MongoCollection, MongoDatabase, Observable, Observer, bson}
import net.liftweb.json.DefaultFormats
import org.mongodb.scala.bson.collection.mutable.Document
import net.liftweb.json.Serialization.write
import org.joda.time.Seconds
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.bson.{BsonDocument, BsonObjectId, BsonValue, ObjectId, conversions}
import org.mongodb.scala.model.{Filters, UpdateOptions}
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.model.Filters._

import scala.concurrent.{Await, Future, duration}
import scala.util.parsing.json.JSON
import scala.util.{Failure, Success}
import schemaTools.schemaValidator._
import utils.AppConfiguration._

import scala.collection.mutable
import scala.concurrent.duration._


object dbDriver extends utils.AkkaActor {

  def insert(inputDocument : Map[String,Any], databaseName : String, collectionName : String, mongoClient
  : MongoClient)
  : Future[Completed] = {

    implicit val formats: DefaultFormats.type = DefaultFormats

    val createdAtUnix = System.currentTimeMillis()

    val insertJson = write( inputDocument ++ Map("createdAtUnix" -> createdAtUnix))
    val insertDocument = Document(insertJson)

    val database: MongoDatabase = mongoClient.getDatabase(databaseName)
    val collection: MongoCollection[Document] = database.getCollection(collectionName)

    collection
      .insertOne(insertDocument)
      .toFuture()

  }

  def updateField(_id : String, inputDocument : Map[String,Any], databaseName : String, collectionName : String,
                  mongoClient
  : MongoClient)
  : Future[UpdateResult] = {

    implicit val formats: DefaultFormats.type = DefaultFormats

    val updateAtUnix = System.currentTimeMillis()

    val updatedJson = write( inputDocument ++ Map("updatedAtUnix" -> updateAtUnix))

    val database: MongoDatabase = mongoClient.getDatabase(databaseName)
    val collection: MongoCollection[Document] = database.getCollection(collectionName)

    val updatedDocument = Document("$set" -> Document(updatedJson))
    collection.updateOne(Filters.eq("_id", BsonObjectId(_id)), updatedDocument)
      .toFuture()

  }


  def pushField(_id : String, inputDocument : Map[String,Any], databaseName : String, collectionName : String,
                mongoClient
                  : MongoClient)
  : Future[UpdateResult] = {

    implicit val formats: DefaultFormats.type = DefaultFormats

    val updatedJson = write( inputDocument)

    val database: MongoDatabase = mongoClient.getDatabase(databaseName)
    val collection: MongoCollection[Document] = database.getCollection(collectionName)

    val updatedDocument = Document("$push" -> Document(updatedJson))

    collection.updateOne(Filters.eq("_id", BsonObjectId(_id)), updatedDocument)
      .toFuture()

  }

  def delete(_id : String, databaseName : String, collectionName : String,
             mongoClient
                  : MongoClient)
  : Future[UpdateResult] = {

    implicit val formats: DefaultFormats.type = DefaultFormats

    val deletedAtUnix = System.currentTimeMillis()

    val updateJson = write( Map("deletedAtUnix" -> deletedAtUnix))

    val database: MongoDatabase = mongoClient.getDatabase(databaseName)
    val collection: MongoCollection[Document] = database.getCollection(collectionName)

    val updateDocument = Document("$set" -> Document(updateJson))
    collection.updateOne(Filters.eq("_id", BsonObjectId(_id)), updateDocument)
      .toFuture()

  }

  def upsert(_id : String,inputDocument : Map[String,Any], databaseName : String, collectionName : String, mongoClient: MongoClient)
  : Future[UpdateResult] = {
    implicit val formats: DefaultFormats.type = DefaultFormats

    val upsertedJson = write( inputDocument)

    val upsertedDocument = Document(upsertedJson)

    val database: MongoDatabase = mongoClient.getDatabase(databaseName)
    val collection: MongoCollection[Document] = database.getCollection(collectionName)

    collection
      .replaceOne(Filters.eq("_id", BsonObjectId(_id)), upsertedDocument,UpdateOptions().upsert(true))
      .toFuture()

  }

  def find(filter : conversions.Bson, collection : MongoCollection[Document], bufferSize : Int) : Option[Document] = {

    var buffer = 0
    var bsonDoc : Document = null

    while (buffer < bufferSize){
      val bsonDocFuture = collection.find(filter).first.toFuture()
      bsonDoc = Await.result(bsonDocFuture, 1.seconds)
      if (bsonDoc == null){
        buffer += 1
      } else {
        buffer = bufferSize
      }
    }
    Some(bsonDoc)
  }
}


