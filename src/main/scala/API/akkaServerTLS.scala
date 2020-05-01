package API

import java.io.{FileInputStream, InputStream}
import java.security.{KeyStore, SecureRandom}

import akka.actor.CoordinatedShutdown
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.Route
import net.liftweb.json.Serialization.write
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.headers.`Content-Type`
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import utils.AppConfiguration._
import akka.http.scaladsl.server.directives.Credentials
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import net.liftweb.json.DefaultFormats
import org.mongodb.scala.{Completed, MongoClient}
import org.mongodb.scala.result.UpdateResult
import schemaTools.schemaValidator._

import scala.concurrent.Future

object akkaServerTLS extends utils.AkkaActor with utils.loggerBase  {

  def main(args: Array[String]): Unit = {
    initializeAppConfig(args(0))
    initializeWebServer()
  }

  def checkAuthentication(credentials: Credentials): Option[String] = credentials match {
    case p @ Credentials.Provided(token) if p.verify(secret) => Some(token)
    case _ => None
  }

  def interfaceAuthenticator(credentials: Credentials): Option[String] =
    credentials match {
      case p @ Credentials.Provided(token) if p.verify(interfaceSecret) => Some(token)
      case _ => None
    }

  def initializeWebServer() = {

    implicit val formats = DefaultFormats

    /*
    INITIALIZE MONGO CLIENTS
     */

    val mongoClient: MongoClient = MongoClient(mongo_uri)

    /*
    SET SSL
     */

    //    val sslConfig = AkkaSSLConfig(system)
    val password: Array[Char] = "password".toCharArray

    val ks: KeyStore = KeyStore.getInstance("PKCS12")
    val keystore: InputStream = new FileInputStream(".cert")
    require(keystore != null, "Keystore required!")
    ks.load(keystore, password)

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
    val httpsContext: HttpsConnectionContext = ConnectionContext.https(sslContext)


    val route: Route =
      pathPrefix("secured") {
        concat(
          /*
        TEST AUTH
         */
          get {
            path("schema-registry" / "test-login") {
              authenticateOAuth2(realm = "secure site", checkAuthentication) { token =>
                complete("Auth OK")
              }
            }
          },

          /*
          INSERT ROUTE
          */

          post {
            path("schema-registry" / "db-driver" / "insert") {
              authenticateOAuth2(realm = "secure site", checkAuthentication) { token =>
                entity(as[String]) { postData => {
                  writeLog("info","db-drive/insert : Post request")
                  val payload = handler.payloadParser.insertPayloadParser(postData, "insert")
                  if (payload.isDefined) {

                    /* Schema validation */
                    writeLog("info","db-drive/insert : Launch schemaValidation method for collection %s".format
                    (payload.get.collection))
                    val payloadData = payload.get
                    val valid = schemaValidation(payloadData.collection, "json", jsonSchemaPath, write(payloadData
                      .document))
                    if (valid.isDefined) {
                      valid.get.status match {
                        case true => {

                          /* Insert document */
                          writeLog("info","db-drive/insert : Schema is valid, Try to insert document to collection " +
                            "%s".format(payloadData.collection))
                          val _id = handler.javaDbDriver.insert(payloadData.document, payloadData.database,
                            payloadData.collection)
                          if (_id.isDefined){
                            val response = handler.responseHandler.buildResponse(
                              _id.get,
                              payloadData.collection,StatusCodes.OK.intValue,
                              s"db-drive/insert : Document successfully inserted" ).get
                            writeLog("info",s"db-drive/insert : Document %s successfully inserted in collection %s"
                              .format(_id.get,payloadData.collection))
                            complete(200,List(`Content-Type`(`application/json`)),response)
                          } else {
                            val response = handler.responseHandler.buildResponse(
                              "Undefined",
                              payloadData.collection,StatusCodes.InternalServerError.intValue,
                              s"db-drive/insert : Unable to insert document" ).get
                            writeLog("error",s"db-drive/insert : Unable to insert document in collection".format
                            (payloadData.collection))

                            complete(500,List(`Content-Type`(`application/json`)),response)
                          }
                        }

                        case false => {
                          val response = handler.responseHandler.buildResponse(
                            "Undefined",
                            payloadData.collection,
                            StatusCodes.BadRequest.intValue,
                            s"""db-drive/insert : Payload schema is not valid :
                               | %s
                               |""".stripMargin.format(valid.get.report)).get

                          writeLog("error",
                            s"""db-drive/insert : Payload schema is not valid :
                               |$response
                               |""".stripMargin)
                          writeLog("error",
                            s"""db-drive/insert : Payload schema is not valid :
                               |%s """.stripMargin.format(valid.get.report))
                          complete(400,List(`Content-Type`(`application/json`)),response)
                        }
                      }
                    } else {
                      val response = handler.responseHandler.buildResponse(
                        "Undefined",
                        payloadData.collection,
                        StatusCodes.InternalServerError.intValue,
                        "db-drive/insert : Unable to get result from schema validation").get

                      writeLog("error","db-drive/insert : Unable to get result from schema validation")
                      complete(500,List(`Content-Type`(`application/json`)),response)

                    }
                  } else {
                    val response = handler.responseHandler.buildResponse(
                      "Undefined",
                      "Unreachabled",
                      StatusCodes.InternalServerError.intValue,
                      "db-drive/insert : Unable to reach post payload").get

                    writeLog("error","db-drive/insert : Unable to reach post payload")
                    complete(500,List(`Content-Type`(`application/json`)),response)
                  }
                }
                }
              }
            }
          },

          /*
          UPDATE ROUTE
           */

          post {
            path("schema-registry" / "db-driver" / "update") {
              authenticateOAuth2(realm = "secure site", checkAuthentication) { token =>
                entity(as[String]) { postData => {
                  writeLog("info","db-drive/update : Post request")
                  val payload = handler.payloadParser.updatePayloadParser(postData, "updateField")
                  if (payload.isDefined) {

                    /* Schema validation */
                    writeLog("info","db-drive/update : Launch schemaValidation method for collection %s".format
                    (payload.get.collection))
                    val payloadData = payload.get
                    val valid = schemaValidation(payloadData.collection, "json", jsonSchemaPath, write(payloadData
                      .document))
                    if (valid.isDefined) {
                      valid.get.status match {
                        case true => {

                          /* Update document */
                          writeLog("info","db-drive/update : Schema is valid, Try to update document in collection " +
                            "%s".format(payloadData.collection))
                          val updateStatus: Future[UpdateResult] = handler.dbDriver.updateField(
                            payloadData._id,
                            payloadData.document,
                            payloadData.database,
                            payloadData.collection,
                            mongoClient)
                          onComplete(updateStatus) { updateResult => {

                            val response = handler.responseHandler.buildResponse(
                              payloadData._id,
                              payloadData.collection,
                              StatusCodes.OK.intValue,
                              s"db-drive/update : Document successfully updated" ).get

                            writeLog("info","db-drive/update : Document %s successfully updated in collection %s".format(payloadData._id, payloadData.collection))
                            complete(200,List(`Content-Type`(`application/json`)),response)

                          }
                          }
                        }
                        case false => {

                          val response = handler.responseHandler.buildResponse(
                            "Undefined",
                            payloadData.collection,
                            StatusCodes.BadRequest.intValue,
                            s"""db-drive/update : Payload schema is not valid :
                               | %s
                               |""".stripMargin.format(valid.get.report)).get

                          writeLog("error",
                            s"""db-drive/update : Payload schema is not valid :
                               |$response
                               |""".stripMargin)
                          writeLog("error",
                            s"""db-drive/update : Payload schema is not valid :
                               |%s """.stripMargin.format(valid.get.report))
                          complete(400,List(`Content-Type`(`application/json`)),response)
                        }
                      }
                    } else {
                      val response = handler.responseHandler.buildResponse(
                        "Undefined",
                        "Unreachabled",
                        StatusCodes.InternalServerError.intValue,
                        "db-drive/update : Unable to get result from schema validation").get

                      writeLog("error","db-drive/update : Unable to get result from schema validation")
                      complete(500,List(`Content-Type`(`application/json`)),response)
                    }
                  } else {
                    val response = handler.responseHandler.buildResponse(
                      "Undefined",
                      "Unreachabled",
                      StatusCodes.InternalServerError.intValue,
                      "db-drive/update : Unable to reach post payload").get

                    writeLog("error","db-drive/update : Unable to reach post payload")
                    complete(500,List(`Content-Type`(`application/json`)),response)
                  }
                }
                }
              }
            }
          },

          /*
          UPSERT ROUTE
           */

          post {
            path("schema-registry" / "db-driver" / "upsert") {
              authenticateOAuth2(realm = "secure site", checkAuthentication) { token =>
                entity(as[String]) { postData => {
                  writeLog("info","db-drive/upsert : Post request")
                  val payload = handler.payloadParser.upsertPayloadParser(postData, "upsert")
                  if (payload.isDefined) {

                    /* Schema validation */
                    writeLog("info","db-drive/upsert : Launch schemaValidation method for collection %s".format
                    (payload.get.collection))
                    val payloadData = payload.get
                    val valid = schemaValidation(payloadData.collection, "json", jsonSchemaPath, write(payloadData
                      .document))
                    if (valid.isDefined) {
                      valid.get.status match {
                        case true => {

                          /* Upsert document */
                          writeLog("info","db-drive/upsert : Schema is valid, Try to upsert document in collection " +
                            "%s".format(payloadData.collection))
                          val updateStatus: Future[UpdateResult] = handler.dbDriver.upsert(
                            payloadData._id,
                            payloadData.document,
                            payloadData.database,
                            payloadData.collection,
                            mongoClient)
                          onComplete(updateStatus) { updateResult => {

                            val response = handler.responseHandler.buildResponse(
                              payloadData._id,
                              payloadData.collection,
                              StatusCodes.OK.intValue,
                              s"db-drive/upsert : Document successfully updated" ).get
                            writeLog("info",("db-drive/upsert : Document %s successfully inserted/updated in collection %s").format(payloadData._id, payloadData.collection))
                            complete(200,List(`Content-Type`(`application/json`)),response)
                          }
                          }
                        }
                        case false => {

                          val response = handler.responseHandler.buildResponse(
                            "Undefined",
                            payloadData.collection,
                            StatusCodes.BadRequest.intValue,
                            s"""db-drive/upsert : Payload schema is not valid :
                               | %s
                               |""".stripMargin.format(valid.get.report)).get

                          writeLog("error",
                            s"""db-drive/upsert : Payload schema is not valid :
                               |$response
                               |""".stripMargin)
                          writeLog("error",
                            s"""db-drive/upsert : Payload schema is not valid :
                               |%s """.stripMargin.format(valid.get.report))
                          complete(400,List(`Content-Type`(`application/json`)),response)
                        }
                      }
                    } else {
                      val response = handler.responseHandler.buildResponse(
                        "Undefined",
                        "Unreachabled",
                        StatusCodes.InternalServerError.intValue,
                        "db-drive/upsert : Unable to get result from schema validation").get

                      writeLog("error","db-drive/upsert : Unable to get result from schema validation")
                      complete(500,List(`Content-Type`(`application/json`)),response)
                    }
                  } else {
                    val response = handler.responseHandler.buildResponse(
                      "Undefined",
                      "Unreachabled",
                      StatusCodes.InternalServerError.intValue,
                      "db-drive/upsert : Unable to reach post payload").get

                    writeLog("error","db-drive/upsert : Unable to reach post payload")
                    complete(500,List(`Content-Type`(`application/json`)),response)
                  }
                }
                }
              }
            }
          },

          /*
PUSH ROUTE
*/

          post {
            path("schema-registry" / "db-driver" / "push") {
              authenticateOAuth2(realm = "secure site", checkAuthentication) { token =>
                entity(as[String]) { postData => {
                  writeLog("info","db-drive/push : Post request")
                  val payload = handler.payloadParser.updatePayloadParser(postData, "updateField")
                  if (payload.isDefined) {
                    /* Update document */
                    val payloadData = payload.get
                    writeLog("info","db-drive/push : Try to update document in collection " +
                      "%s".format(payloadData.collection))
                    val updateStatus: Future[UpdateResult] = handler.dbDriver.pushField(
                      payloadData._id,
                      payloadData.document,
                      payloadData.database,
                      payloadData.collection,
                      mongoClient)
                    onComplete(updateStatus) { _ => {

                      val response = handler.responseHandler.buildResponse(
                        payloadData._id,
                        payloadData.collection,
                        StatusCodes.OK.intValue,
                        s"db-drive/push : Document successfully updated" ).get

                      writeLog("info","db-drive/push : Document %s successfully updated in collection %s".format
                      (payloadData._id, payloadData.collection))
                      complete(200,List(`Content-Type`(`application/json`)),response)
                    }
                    }
                  } else {
                    val response = handler.responseHandler.buildResponse(
                      "Undefined",
                      "Unreachabled",
                      StatusCodes.InternalServerError.intValue,
                      "db-drive/push : Unable to reach post payload").get

                    writeLog("error","db-drive/push : Unable to reach post payload")
                    complete(500,List(`Content-Type`(`application/json`)),response)
                  }
                }
                }
              }
            }
          },


          /*
          ADMIN ROUTE
           */
          post {
            path("schema-registry" / "admin" / "schema-update") {
              authenticateOAuth2(realm = "secure site", checkAuthentication) { token =>
                entity(as[String]) { postData => {
                  writeLog("info","admin/schema-update : Post request for schema update")
                  val payload = admin.schemaUpdate.parsePayload(postData)
                  if (payload.isDefined) {
                    admin.schemaUpdate.deploySchema(payload.get)
                    writeLog("info","admin/schema-update : Schema updated for collection %s".format(payload.get.collection))
                    complete(HttpResponse(StatusCodes.OK, entity = s"Schema updated for collection %s".format(payload.get.collection)))

                  } else {
                    writeLog("error","admin/schema-update : Unable to reach post payload")
                    complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Unable to reach post payload"))
                  }
                }
                }
              }
            }
          },

          /*
          DELETE ROUTE
          */

          post {
            path("schema-registry" / "db-driver" / "delete") {
              authenticateOAuth2(realm = "secure site", checkAuthentication) { token =>
                entity(as[String]){postData => {
                  writeLog("info","db-drive/delete : Post request")
                  val payload = handler.payloadParser.deletePayloadParser(postData, "delete")
                  if(payload.isDefined){
                    val payloadData = payload.get
                    val updateStatus : Future[UpdateResult] = handler.dbDriver.delete(
                      payloadData._id,
                      payloadData.database,
                      payloadData.collection,
                      mongoClient)
                    onComplete(updateStatus){ updateResult=> {

                      val response = write(handler.responseHandler.buildResponse(
                        payloadData._id,
                        payloadData.collection,
                        StatusCodes.OK.intValue,
                        "db-drive/delete : Document successfully marked as deleted").get)

                      writeLog("info","db-drive/delete : Document {%s} successfully marked as deleted".format(payloadData._id))
                      complete(200,List(`Content-Type`(`application/json`)),response)
                    }}
                  } else {
                    val response = handler.responseHandler.buildResponse(
                      "Undefined",
                      "Unreachabled",
                      StatusCodes.InternalServerError.intValue,
                      "db-drive/delete : Unable to reach post payload").get

                    writeLog("error","db-drive/delete : Unable to reach post payload")
                    complete(500,List(`Content-Type`(`application/json`)),response)
                  }
                }}
              }
            }
          },
          get {
            path("schema-registry" / "schema-interface" / "json") {
              authenticateBasic(realm = "secure site", interfaceAuthenticator) { userName =>
                parameters('collection) {
                  (collection) => {
                    val schema = schemaTools.schemaToolsUtils.getSchema(collection, "json", jsonSchemaPath)
                    writeLog("info","GET -> $collection")
                    if (schema.isDefined) {
                      complete(HttpEntity(ContentTypes.`application/json`, schema.get))
                    } else {
                      writeLog("error","schema-interface/json : Unable to display schema for " +
                        s"collection $collection")
                      complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Unable to display schema for collection $collection"))
                    }
                  }
                }
              }
            }
          }
        )
      }

    val bindingFuture = Http().bindAndHandle(route, interface, port.toInt)
    val bindingFutureHttps = Http().bindAndHandle(route, interface = interface, port = portHttps.toInt, connectionContext = httpsContext)

    println(s"Server online at http://$interface:$port/")
    println(s"Server online at https://$interface:$portHttps/")

    CoordinatedShutdown(system).addJvmShutdownHook({
      bindingFuture
        .flatMap(_.unbind())
    })

    CoordinatedShutdown(system).addJvmShutdownHook({
      bindingFutureHttps
        .flatMap(_.unbind())
    })
  }

}
