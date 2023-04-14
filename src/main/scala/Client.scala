
import Client.ListingResponse
import FileReaderStarter.{ListingResponse, serviceKey}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.util.LineNumbers.Result
import Store._
import akka.NotUsed
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.Behaviors

object Client {
  sealed trait Command
  case class Get(key:String) extends Command
  case class Set(currentBatch:List[(String,String)]) extends Command
  case class Count() extends Command

  private case class ListingResponse(listing: Receptionist.Listing) extends Command

  val clientServiceKey: ServiceKey[Command] = ServiceKey[Command]("clientService")

  def apply():Behavior[Client.Command]=
    Behaviors.setup { context =>
      println("client waiting for creation")
      val listingResponseAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse.apply)
      context.system.receptionist ! Receptionist.Subscribe(Store.storeServiceKey, listingResponseAdapter)
      new Client(context,None)
    }

  def apply(store: ActorRef[Store.Command]): Behavior[Command] = {
    Behaviors.setup { context =>
      context.system.receptionist ! Receptionist.Register(clientServiceKey,context.self)
      println("Creating Clients")
      new Client(context,Some(store))
    }
  }
}


class  Client  (context: ActorContext[Client.Command], connectedStore:Option[ActorRef[Store.Command]])extends AbstractBehavior[Client.Command](context) {
  import Client._

  override def onMessage(message: Command): Behavior[Command] = message match {
    case Get(key: String) => {
      val responseActor=context.spawnAnonymous(Responses())
      val store = connectedStore.get
      store ! Store.Get(responseActor,key.getBytes())
      Behaviors.same
    }
    case Count() => {
      val responseActor = context.spawnAnonymous(Responses())
      val store = connectedStore.get
      store ! Store.Count(responseActor)
      Behaviors.same
    }
    case Set(currentBatch) => {
      //hier ganzes batch senden
      val store = connectedStore.get
      currentBatch.foreach(entry=>
        store ! Store.Set(context.spawnAnonymous(Responses()), entry._1.getBytes(), entry._2.getBytes())
      )
      Behaviors.same
    }
    case ListingResponse(listing) => {
      //spawn one reader and make it send messages to every client
      val stores = listing.serviceInstances(Store.storeServiceKey)
      stores.foreach(store => context.spawnAnonymous(Client(store)))

      Behaviors.same
    }
    case _ => {
      context.log.info("Faulty Message (to Client)")
      context.log.info(message.toString)
      Behaviors.same
    }

  }
}




