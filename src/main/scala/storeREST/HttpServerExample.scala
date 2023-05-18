package storeREST

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives.{complete, path}




object HttpServerExample {
  def main(args:Array[String]):Unit={

    implicit val system = ActorSystem(Behaviors.empty,"hfu")
    implicit  val executionContext=system.executionContext

    val route = {
      path("hello"){
        Directives.get{
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,  "<h1>hallo from my akka</h1>"))
        }
      }
    }
    val bindingFuture = Http().newServerAt("localhost",8080).bind(route)
    println("starting server at http://localhost:8888/hello ")
    scala.io.StdIn.readLine()
    bindingFuture.flatMap(_.unbind()).onComplete(_=> system.terminate())


  }
}
