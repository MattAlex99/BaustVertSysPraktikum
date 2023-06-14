package storeREST

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.Done
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
// for JSON serialization/deserialization following dependency is required:
// "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7"
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.io.StdIn

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object HttpServer{
  //Klasse die HTTP Server Funktionalität umsetzt (senden und empfangen via HTTP)

  final case class Item(key: String, value:String)

}

class HttpServer(usedSystem: ActorSystem[_],setGetReciever:HttpServerComplete) {
  import HttpServer._

  // needed to run the route
  implicit val system: ActorSystem[_] = usedSystem

  // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
  implicit val executionContext: ExecutionContext = system.executionContext
  implicit val itemFormat: RootJsonFormat[Item] = jsonFormat2(Item.apply)


  def fetchItemByPromise(itemKey: String): Future[Option[Item]] = Future {
    val promise=setGetReciever.getKVFuture(itemKey)
    Await.result(promise,5.seconds)
  }


  def saveOrder(item: Item): Future[Done] = {
    setGetReciever.Setkv(item.key,item.value)
    Future {
     Done
    }
  }

  def run(host:String,port:Integer): Unit = {
    val route: Route =
      concat(
        get {
          pathPrefix("health-check") {
            complete("is alive")
          }
          pathPrefix("get" / Segment) { key =>
            // there might be no item for a given id
            val maybeItem: Future[Option[Item]] = fetchItemByPromise(key)
            onSuccess(maybeItem) {
              case Some(item) =>
                complete(item)
              case None =>
                complete(StatusCodes.NotFound)
              case _ =>
                complete("nothing was fetched")
            }
          }
        },
        post {
          path("set") {
            entity(as[Item]) { order =>
              val saved: Future[Done] = saveOrder(order)
              onSuccess(saved) { _ => // we are not interested in the result value `Done` but only in the fact that it was successful
                complete("order created")
              }
            }
          }
        }
      )

    val bindingFuture = Http().newServerAt(host, port).bind(route)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

}
