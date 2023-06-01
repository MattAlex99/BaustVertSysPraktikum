package storeGRCP

import akkaStore._
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akkaStore.{Responses, Store}
import de.hfu.protos.messages.{GetReply, GrpcClientGrpc, SetReply}
import io.grpc.ServerBuilder

import java.util.logging.Logger
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}
object GrcpServer{
  sealed trait ServerCommand

  val serviceKey: ServiceKey[ServerCommand] = ServiceKey[ServerCommand]("GrcpServerKey")
  case class ListingResponse(listing: Receptionist.Listing) extends ServerCommand

  def apply(port: Int, host: String): Behavior[ServerCommand] =
    Behaviors.setup { context =>
      println("storeGRCP.GrcpServer waiting for creation")
      val listingResponseAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse.apply)
      context.system.receptionist ! Receptionist.Subscribe(Store.storeServiceKey, listingResponseAdapter)
      new GrcpServerStartup(context, port, host)
    }

  def apply(port: Int, host: String, storeRef:ActorRef[Store.Command]): Behavior[ServerCommand] =
    Behaviors.setup { context =>
      println("storeGRCP.GrcpServer found store and will create itself")
      new GrcpServer(context, port, host,storeRef)
    }
}

class  GrcpServerStartup  (context: ActorContext[GrcpServer.ServerCommand], port:Int, host:String)extends AbstractBehavior[GrcpServer.ServerCommand](context) {
  import GrcpServer._
  override def onMessage(msg: ServerCommand): Behavior[ServerCommand] = msg match {

    case ListingResponse(listing) => {
      println("store found")
      val stores = listing.serviceInstances(Store.storeServiceKey)
      print(stores)
      val store = stores.headOption
      store match {
        case Some(storeRef) => context.spawnAnonymous(storeGRCP.GrcpServer(port,host,storeRef))
        case _ => println("store couldn't be started")
      }
      Behaviors.same
    }
  }
}




class  GrcpServer  (context: ActorContext[GrcpServer.ServerCommand],
                    port:Int, host:String, store:ActorRef[Store.Command]
                   )extends AbstractBehavior[GrcpServer.ServerCommand](context) {
  import GrcpServer._
  import akka.util.Timeout

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger.getLogger(GrcpServer.getClass.getName)
  logger.info("booting up server")

  //start a GRCPClientImpl, that will recieve messages from the GRCP Client and pass it on to the GRCP Server
  val service = GrpcClientGrpc.GrpcClient.bindService(new GrcpClientImpl(this,context), ExecutionContext.global)
  val server = ServerBuilder
    .forPort(port)
    .addService(service)
    .asInstanceOf[ServerBuilder[_]]
    .build()
    .start()
  logger.info("server Starting, listening on port "+ port)


  implicit val timeout: Timeout = Timeout(3.seconds)
  implicit val scheduler = context.system.scheduler


  def setKVFuture(key: String,value:String): Future[SetReply] = {
    //implicits arent srictly necessairy here as they are already defined in the class, but I want to
    //make it obvious that this method uses impicits
    implicit val timeout: Timeout = Timeout(5.seconds)
    implicit val scheduler = context.system.scheduler
    val reply = store ? (replyTo => Store.Set(replyTo, key.getBytes().toSeq, value.getBytes().toSeq))
    val promise = Promise[SetReply]
    reply.onComplete {
      case Success(response) =>
        //extract the relevant information on succes
        val casted_response = response.asInstanceOf[Responses.SetResult]
        val item = SetReply(Utils.customByteToString(casted_response.key), Utils.customByteToString(casted_response.value))
        promise.success(item)
      case Failure(exception: Exception) =>
        exception.printStackTrace()
        promise.failure(exception)
      case _ =>
        None
    }
    promise.future
  }
  def getKVFuture(key: String): Future[GetReply] = {
    //implicits arent srictly necessairy here as they are already defined in the class, but I want to
    //make it obvious that this method uses impicits
    implicit val timeout: Timeout = Timeout(5.seconds)
    implicit val scheduler = context.system.scheduler
    val reply = store ? (replyTo => Store.Get(replyTo, key.getBytes().toSeq))
    val promise = Promise[GetReply]
    //cover both successful and failed remote call
    reply.onComplete {
      case Success(response) =>
        val casted_response = response.asInstanceOf[Responses.GetResultSuccessful]
        casted_response.value match {
          case Some(value) =>
            //if the set value has a vlaid Key, extract relevant info
            val item = GetReply(Utils.customByteToString(casted_response.key), Some(Utils.customByteToString(value)))
            promise.success(item)
          case _ =>
            //if thr relevant item is not found  create a reply with None as value
            val item = GetReply(Utils.customByteToString(casted_response.key),None)
            promise.success(item)
        }
      case Failure(exception: Exception) =>
        exception.printStackTrace()
        None
      case _ => None
    }
    promise.future
  }



  override def onMessage(message: ServerCommand): Behavior[ServerCommand] = message match {
    case _ => {
      context.log.info("This actor Takes no Messages (to akkaStore.Client)")
      context.log.info(message.toString)
      Behaviors.same
    }

  }
}




