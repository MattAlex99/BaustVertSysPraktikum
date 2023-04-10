import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import scala.util.Random


object ClientStarter {

  sealed trait Command

  private case class ListingResponse(listing: Receptionist.Listing) extends Command
  val serviceKey: ServiceKey[Command] = ServiceKey[Command]("clientStarterService")

  def apply(namePrefix:String): Behavior[Command] = {
    Behaviors.setup { context =>
      val listingResponseAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse.apply)
      context.system.receptionist ! Receptionist.Register(serviceKey, context.self)
      context.system.receptionist ! Receptionist.Subscribe(Store.storeServiceKey, listingResponseAdapter)
      new ClientStarter(context,namePrefix)
    }
  }
}

class  ClientStarter  (context: ActorContext[ClientStarter.Command],namePrefix:String)
  extends AbstractBehavior[ClientStarter.Command](context) {

  import ClientStarter._


  override def onMessage(message: Command): Behavior[Command] = message match {

    case ListingResponse(listing)=>{
      //spawns one Client for every store that exists and ends actor
      val stores = listing.serviceInstances(Store.storeServiceKey)
      stores.foreach(store => context.spawn(Client(store),namePrefix+Integer.toString(Random.nextInt(13))))
      Behaviors.same //TODO rausfinden warum ich hier nicht beenden kann
  }

}
}
