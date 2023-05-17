
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import io.grpc.ServerBuilder

import java.util.logging.Logger
import scala.concurrent.{ExecutionContext, Future}
import de.hfu.protos
import de.hfu.protos.messages.GrpcClientGrpc

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}
object GrcpServer{
  sealed trait ServerCommand
  case class Set(key:String,value: String,replyTo: ActorRef[Responses.Result]) extends ServerCommand


  val serviceKey: ServiceKey[ServerCommand] = ServiceKey[ServerCommand]("GrcpServerKey")
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
      println("store found")
      val stores = listing.serviceInstances(Store.storeServiceKey)
      print(stores)
      val store = stores.headOption
      store match {
        case Some(storeRef) => context.spawnAnonymous(GrcpServer(port,host,storeRef))
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
  import scala.concurrent.ExecutionContext.Implicits.global
  import akka.util.Timeout
  import scala.concurrent.duration.Duration

  val logger = Logger.getLogger(GrcpServer.getClass.getName)
  logger.info("booting up server")
  val service = GrpcClientGrpc.GrpcClient.bindService(new GrcpClientImpl(this,context.self,context), ExecutionContext.global)
  val server = ServerBuilder
    .forPort(port)
    .addService(service)
    .asInstanceOf[ServerBuilder[_]]
    .build()
    .start()
  logger.info("server Starting, listening on port "+ port)


  implicit val timeout: Timeout = Timeout(3.seconds)
  implicit val scheduler = context.system.scheduler
  def Setkv(key:String,value:String):Future[Responses.Result]={
    val result = store ? (replyTo => Store.Set(replyTo, key.getBytes().toSeq, value.getBytes().toSeq))
    return result
  }

  def getKV(key:String):Future[Responses.Result]={
    val result = store ? (replyTo => Store.Get(replyTo, key.getBytes().toSeq))
    return result
  }


  override def onMessage(message: ServerCommand): Behavior[ServerCommand] = message match {
    case Set(key:String,value:String,replyTo: ActorRef[Responses.Result])=>{
      logger.info("in set Key")
      val replyTo = context.spawnAnonymous(Responses()) // hier darf ich scheinbar keinen aktor spawnen, muss ich in message machen
      //hier mit ask arbeiten
      logger.info("pre")
      //store ! Store.Set(replyTo, key.getBytes().toSeq, value.getBytes().toSeq)

      //val result = store ? Store.Set(replyTo, key.getBytes().toSeq, value.getBytes().toSeq)
      val result = store ? (replyTo => Store.Set(replyTo, key.getBytes().toSeq, value.getBytes().toSeq))
      logger.info("post")

      result.onComplete {
        case Success(value) => logger.info("got ask response")
        logger.info(result.toString)
        case Failure(exception) => exception.printStackTrace()
      }
      Behaviors.same
    }
    case _ => {
      context.log.info("Faulty Message (to Client)")
      context.log.info(message.toString)
      Behaviors.same
    }

  }
}




