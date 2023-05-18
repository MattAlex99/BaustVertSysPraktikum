package storeREST

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.Done
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes

import scala.collection.mutable.ListBuffer
// for JSON serialization/deserialization following dependency is required:
// "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7"
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.io.StdIn

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object HttpServer{
  final case class Item(key: String, value:String)

}

class HttpServer(usedSystem: ActorSystem[_],setGetReciever:HttpServerComplete) {
  import HttpServer._

  // needed to run the route
  implicit val system: ActorSystem[_] = usedSystem
  // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
  implicit val executionContext: ExecutionContext = system.executionContext


  //val newItem:Item= Item("abcd",42)
  //saveOrder(Order( newItem::Nil ))

  // domain model

  //final case class Order(items: List[Item])

  // formats for unmarshalling and marshalling
  implicit val itemFormat: RootJsonFormat[Item] = jsonFormat2(Item.apply)
  //implicit val orderFormat: RootJsonFormat[Order] = jsonFormat1(Order.apply)

  // (fake) async database query api
  def fetchItem(itemKey: String): Future[Option[Item]] = Future {
    val response = setGetReciever.getKV(itemKey)
    println("fetched response:",response)

    val weirdResults: ListBuffer[Item] = ListBuffer.empty[Item]
    var returnValue:Option[Item]=None
    response match {
      case Some(value) =>
        println("item was fetched",value)
        //TODO Fragen warum zur Hölle dashier funktioniert und ein einfacher return nicht
        weirdResults+=value
        returnValue=Some(value)
        //return  Future.successful(Some(Item("a","b")))
      case None =>
        println("item wasnt fetched")
        return Future.successful(None)
    }
    println("default case in fetch")
    None
    //replace with call to SetGetReciever
    println(weirdResults)
    //weirdResults.find(o => o.key == itemKey)
    returnValue

  }

  def saveOrder(item: Item): Future[Done] = {
    setGetReciever.Setkv(item.key,item.value)

    //replace with call to SetGetReciever
    //orders = item ::: orders
    Future {
     Done
    }
  }

  def run(host:String,port:Integer): Unit = {
    val route: Route =
      concat(
        get {
          pathPrefix("get" / Segment) { key =>
            // there might be no item for a given id
            val maybeItem: Future[Option[Item]] = fetchItem(key)

            onSuccess(maybeItem) {
              case Some(item) =>
                println("webside displays item")
                complete(item)
              case None =>
                println("no Item to display was found")
                println("maybeItem",maybeItem)
                complete(StatusCodes.NotFound)
              case _ =>
                println("Webside deiplays nothing")
                complete("nothing was fetched")
            }
          }
        },
        post {
          path("create-order") {
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
    //curl -H "Content-Type: application/json" -X POST -d {\"items\":[{\"name\":\"hhgtg\",\"id\":42}]} http://localhost:8080/create-order
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

}
