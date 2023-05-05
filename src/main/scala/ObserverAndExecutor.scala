import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding

object ObserverAndExecutor {
  sealed trait Command

  //starts printing of Results
  case class ExecuteTests() extends Command

  //Finds a Store and passes it on to FindFileReader
  case class StartReading() extends Command


 val serviceKey: ServiceKey[Command] = ServiceKey[Command]("observeAndExecuteKey")

  case class ListingResponse(listing: Receptionist.Listing) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      println("Creating Client Observer")
      val listingResponseAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse.apply)
      context.system.receptionist ! Receptionist.Subscribe(Client.clientServiceKey, listingResponseAdapter)
      new ObserverAndExecutorSimple(context)
    }

  def apply(clientRef: ActorRef[Client.Command]): Behavior[Command] =
    Behaviors.setup { context =>
      println("Creating Filereader Observer")
      val listingResponseAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse.apply)
      context.system.receptionist ! Receptionist.Subscribe(FileReader.serviceKey, listingResponseAdapter)
      new ObserverAndExecutorWithClient(context,clientRef)
    }

  def apply(clientRef: ActorRef[Client.Command],readerRef: ActorRef[FileReader.Message]): Behavior[Command] =
    Behaviors.setup { context =>
      println("Creating Complete Observer")

      //Do something that fetches a file Reader
      new ObserverAndExecutorComplete(context,clientRef,readerRef)
    }
}


class  ObserverAndExecutorSimple (context: ActorContext[ObserverAndExecutor.Command]) extends AbstractBehavior[ObserverAndExecutor.Command](context) {
  import ObserverAndExecutor._
  //fetches a client ref
  override def onMessage(msg: ObserverAndExecutor.Command): Behavior[ObserverAndExecutor.Command] = msg match {
    case ListingResponse(listing) => {
      //fetches a client ref and creates a new ObserverAndExecutor
      val clients = listing.serviceInstances(Client.clientServiceKey)
      val client = clients.headOption
      client match {
        case Some(clientRef) =>
          context.spawnAnonymous(ObserverAndExecutor(clientRef))
        case _ =>
      }
      Behaviors.same
    }
  }
}

class  ObserverAndExecutorWithClient (context: ActorContext[ObserverAndExecutor.Command], clientRef:ActorRef[Client.Command]) extends AbstractBehavior[ObserverAndExecutor.Command](context) {
  import ObserverAndExecutor._
  //fetches a FileReader ref
  override def onMessage(msg: ObserverAndExecutor.Command): Behavior[ObserverAndExecutor.Command] = msg match {
    case ListingResponse(listing) => {
      //fetches a client ref and creates a new ObserverAndExecutor
      val fileReader= listing.serviceInstances(FileReader.serviceKey)
      val reader = fileReader.headOption
      reader match {
        case Some(readerRef) =>
          val completeObserver = context.spawnAnonymous(ObserverAndExecutor(clientRef,readerRef))
          completeObserver ! ObserverAndExecutor.ExecuteTests()
        case _ =>
      }
      Behaviors.same
    }
  }
}

class ObserverAndExecutorComplete (context: ActorContext[ObserverAndExecutor.Command],
                                    clientRef:ActorRef[Client.Command],
                                    readerRef:ActorRef[FileReader.Message]) extends AbstractBehavior[ObserverAndExecutor.Command](context) {
  val filename = "../trip_data_1000_000.csv"
  import ObserverAndExecutor._

  override def onMessage(msg: ObserverAndExecutor.Command): Behavior[ObserverAndExecutor.Command] = msg match {
    case StartReading() => {
      println("will tell reader to read")
      readerRef ! FileReader.File(filename, clientRef)
      Behaviors.same
    }
    case ExecuteTests() => {
      println("getting values")
      clientRef ! Client.Get("89D227B655E5C82AECF13C3F540D4CF4") //
      clientRef ! Client.Get("2BE8AF8C2AAA9791C731972187AAAE80") //
      clientRef ! Client.Get("B5B2CB8B79EBD8F8FB54CD275BCC9BB6") //
      clientRef ! Client.Get("0A6716952AE1B7B1EB901776A8DB82C8") //
      clientRef ! Client.Get("0B18AD7511A013CB08333F312AFDEC96") //

      val sharding= ClusterSharding(context.system)
      Range.inclusive(0,Store.numOfUsedShards+2).foreach(
        shardId=>{
          sharding.entityRefFor(StoreShard.TypeKey,shardId.toString) ! StoreShard.PrintInfo()
        }
      )

      Behaviors.same
    }


  }
}

