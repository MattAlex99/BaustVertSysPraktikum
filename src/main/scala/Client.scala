
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.util.LineNumbers.Result
import Store._
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.Behaviors

object Client {
  sealed trait Command
  case class Get(key:String) extends Command
  case class Set(currentBatch:List[(String,String)]) extends Command
  case class Count() extends Command


  val clientServiceKey: ServiceKey[Command] = ServiceKey[Command]("clientService")

  def apply(store: ActorRef[Store.Command]): Behavior[Command] = {
    Behaviors.setup { context =>
      context.system.receptionist ! Receptionist.Register(clientServiceKey,context.self)
      new Client(context,store)
    }
  }
}


class  Client  (context: ActorContext[Client.Command], connectedStore:ActorRef[Store.Command])extends AbstractBehavior[Client.Command](context) {
  import Client._

  override def onMessage(message: Command): Behavior[Command] = message match {
    case Get(key: String) => {
      val responseActor=context.spawnAnonymous(Responses())
      connectedStore ! Store.Get(responseActor,key.getBytes())
      Behaviors.same
    }
    case Count() => {
      val responseActor = context.spawnAnonymous(Responses())
      connectedStore ! Store.Count(responseActor)
      Behaviors.same
    }
    case Set(currentBatch) => {
      //hier ganzes batch senden
      currentBatch.foreach(entry=>
        connectedStore ! Store.Set(context.spawnAnonymous(Responses()), entry._1.getBytes(), entry._2.getBytes())
      )
      Behaviors.same
    }

    case _ => {
      context.log.info("Faulty Message (to Client)")
      context.log.info(message.toString)
      Behaviors.same

    }
  }


}
