import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import scala.util.Random


object FileReaderStarter {

  sealed trait Command

  private case class ListingResponse(listing: Receptionist.Listing) extends Command
  val serviceKey: ServiceKey[Command] = ServiceKey[Command]("fileReaderStarterService")

  def apply(namePrefix:String): Behavior[Command] = {
    Behaviors.setup { context =>
      val listingResponseAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse.apply)
      context.system.receptionist ! Receptionist.Register(serviceKey, context.self)
      context.system.receptionist ! Receptionist.Subscribe(Client.clientServiceKey, listingResponseAdapter)
      println("starting FileReader Starter and registering ")
      new FileReaderStarter(context,namePrefix)
    }
  }
}

class  FileReaderStarter  (context: ActorContext[FileReaderStarter.Command],namePrefix:String)
  extends AbstractBehavior[FileReaderStarter.Command](context) {

  import FileReaderStarter._
  override def onMessage(message: Command): Behavior[Command] = message match {
    case ListingResponse(listing)=>{
      //spawn one reader and make it send messages to every client
      val clients = listing.serviceInstances(Client.clientServiceKey)
      val reader =context.spawn(FileReader(),namePrefix+Integer.toString(Random.nextInt(10)))

      clients.foreach(client => reader ! FileReader.File("./trip_data_100.csv", client))
      Behaviors.same //TODO rausfinden warum ich hier nicht beenden kann

    }


  }
}
