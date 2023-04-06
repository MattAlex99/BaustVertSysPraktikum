
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.util.LineNumbers.Result
import Store._

object Client {
  sealed trait Command
  case class Get(key:String) extends Command
  case class Set(key:String, value:String) extends Command

  def apply(store: ActorRef[Store.Command]): Behavior[Command] = {
    Behaviors.setup { context =>
      new Client(context,store)
    }
  }
}


class  Client private (context: ActorContext[Client.Command], connectedStore:ActorRef[Store.Command])extends AbstractBehavior[Client.Command](context) {
  import Client._
  override def onMessage(message: Command): Behavior[Command] = message match {
    case Get(key: String) => {
      val responseActor=context.spawnAnonymous(Responses())
      connectedStore ! Store.Get(responseActor,key.getBytes())
      Behaviors.same

    }
    case Set(key:String, value:String) => {
      val responseActor=context.spawnAnonymous(Responses())
      connectedStore ! Store.Set(responseActor,key.getBytes(),value.getBytes())
      Behaviors.same

    }
    case _ => {
      context.log.info("not yet implemented")
      Behaviors.same

    }
  }


}
