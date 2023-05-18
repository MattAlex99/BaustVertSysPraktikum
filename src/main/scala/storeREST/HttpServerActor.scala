package storeREST




import akkaStore._
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import akkaStore.{Responses, Store}
import de.hfu.protos.messages.GrpcClientGrpc
import io.grpc.ServerBuilder
import storeGRCP.GrcpServer.ServerCommand

import java.util.logging.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}
object HttpServerActor{
  sealed trait HttpServerCommand

  val serviceKey: ServiceKey[HttpServerCommand] = ServiceKey[HttpServerCommand]("GrcpServerKey")
  case class ListingResponse(listing: Receptionist.Listing) extends HttpServerCommand



  def apply(port: Int, host: String): Behavior[HttpServerCommand] =
    Behaviors.setup { context =>
      println("HttpServer waiting for creation")
      val listingResponseAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse.apply)
      context.system.receptionist ! Receptionist.Subscribe(Store.storeServiceKey, listingResponseAdapter)
      new HttpServerStartup(context, port, host)
    }

  def apply(port: Int, host: String, storeRef:ActorRef[Store.Command]): Behavior[HttpServerCommand] =
    Behaviors.setup { context =>
      println("HttpServer found store and will create itself")
      new HttpServerComplete(context, port, host,storeRef)
    }
}

class  HttpServerStartup  (context: ActorContext[HttpServerActor.HttpServerCommand], port:Int, host:String)extends AbstractBehavior[HttpServerActor.HttpServerCommand](context) {
  import HttpServerActor._
  override def onMessage(msg: HttpServerCommand): Behavior[HttpServerCommand] = msg match {

    case ListingResponse(listing) => {
      println("store found")
      val stores = listing.serviceInstances(Store.storeServiceKey)
      print(stores)
      val store = stores.headOption
      store match {
        case Some(storeRef) => context.spawnAnonymous(HttpServerActor(port,host,storeRef))
        case _ => println("store couldn't be started")
      }
      Behaviors.same
    }
  }
}




class  HttpServerComplete  (context: ActorContext[HttpServerActor.HttpServerCommand],
                            port:Int, host:String, store:ActorRef[Store.Command]
                   )extends AbstractBehavior[HttpServerActor.HttpServerCommand](context) {
  import HttpServerActor._
  import storeREST.HttpServer.Item
  val localHttpServer = new  HttpServer(context.system,this)
  localHttpServer.run(host,port)


  def Setkv(key: String, value: String):Unit = {
    implicit val timeout: Timeout = Timeout(5.seconds)
    implicit val scheduler = context.system.scheduler
    val result = store ? (replyTo => Store.Set(replyTo, key.getBytes().toSeq, value.getBytes().toSeq))
  }

  def getKVFuture(key:String):Future[Option[Item]] ={
    implicit val timeout: Timeout = Timeout(5.seconds)
    implicit val scheduler = context.system.scheduler
    val reply = store ? (replyTo => Store.Get(replyTo, key.getBytes().toSeq))
    println("get reply:", reply)
    val promise = Promise[Option[Item]]
    reply.onComplete {
      case Success(response) =>
        println("complete future")
        val casted_response = response.asInstanceOf[Responses.GetResultSuccessful]
        casted_response.value match {
          case Some(value) =>
            val item = Item(Utils.customByteToString(casted_response.key), Utils.customByteToString(value))
            promise.success(Some(item))
          case _ =>
            println("uncomplete Future")
            promise.success(None)
        }
      case Failure(exception: Exception) =>
        exception.printStackTrace()
         None
      case _ => None
    }
    print("primse",promise)
    promise.future
  }



  override def onMessage(message: HttpServerCommand): Behavior[HttpServerCommand] = message match {
    case _ =>
      println("HttpServerActors dont take messages")
      Behaviors.same
  }
}




