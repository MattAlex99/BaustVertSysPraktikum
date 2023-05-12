package storeGRCP

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import de.hfu.protos.messages.GrpcClientGrpc

import java.util.logging.Logger
import io.grpc.ServerBuilder

import java.util.logging.Logger
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import storeGRCP.GrcpClientImpl

import _root_.Store
object GrcpServer{
  sealed trait ServerCommand

  val serviceKey: ServiceKey[ServerCommand] = ServiceKey[ServerCommand]("observeAndExecuteKey")
  case class ListingResponse(listing: Receptionist.Listing) extends ServerCommand

  def apply(port: Int, host: String): Behavior[ServerCommand] =
    Behaviors.setup { context =>
      println("GrcpServer waiting for creation")
      val listingResponseAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse.apply)
      context.system.receptionist ! Receptionist.Subscribe(Store.storeServiceKey, listingResponseAdapter)
      new GrcpServerStartup(context, port, host)
    }

  def apply(port: Int, host: String, storeRef:ActorRef[Store.Command]): Behavior[ServerCommand] =
    Behaviors.setup { context =>
      println("GrcpServer found store and will create itself")

      new GrcpServer(context, port, host,storeRef)
    }
}

class  GrcpServerStartup  (context: ActorContext[GrcpServer.ServerCommand], port:Int, host:String)extends AbstractBehavior[GrcpServer.ServerCommand](context) {
  import GrcpServer._
  override def onMessage(msg: ServerCommand): Behavior[ServerCommand] = msg match {
    case ListingResponse(listing) => {
      //fetches a client ref and creates a new ObserverAndExecutor
      val stores = listing.serviceInstances(Store.storeServiceKey)
      val store = stores.headOption
      store match {
        case Some(storeRef) => context.spawnAnonymous(GrcpServer(port,host,storeRef))
        case _ =>
      }
      Behaviors.same
    }
  }
}




class  GrcpServer  (context: ActorContext[GrcpServer.ServerCommand],
                    port:Int, host:String, store:ActorRef[Store.Command]
                   )extends AbstractBehavior[GrcpServer.ServerCommand](context) {
  import GrcpServer._

  val logger = Logger.getLogger(GrcpServer.getClass.getName)
  logger.info("booting up server")
  val service = GrpcClientGrpc.GrpcClient.bindService(new GrcpClientImpl(this), ExecutionContext.global)
  val server = ServerBuilder
    .forPort(port)
    .addService(service)
    .asInstanceOf[ServerBuilder[_]]
    .build()
    .start()
  logger.info("server Starting, listening on port "+ port)

  def setKey(key:String,value:String)={
    logger.info("in set Key")
  }
  override def onMessage(message: ServerCommand): Behavior[ServerCommand] = message match {
    case _ => {
      context.log.info("Faulty Message (to Client)")
      context.log.info(message.toString)
      Behaviors.same
    }

  }
}