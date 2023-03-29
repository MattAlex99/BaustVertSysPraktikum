import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.util.LineNumbers.Result

import java.nio.charset.StandardCharsets

object Responses {
  sealed trait Result

  case class SetResult(key: String, value: String) extends Result
  case class GetResult(key: String, value: String) extends Result

  def apply(): Behavior[Result] = {
    Behaviors.setup { context =>
      new Responses(context)
    }
  }
}


class Responses(context: ActorContext[Responses.Result])extends AbstractBehavior[Responses.Result](context) {
  //TODO Wäre es klug den aktor direkt zu beenden nachdem er den Wert angezeigt hat?
  import Responses._
  override def onMessage(message: Responses.Result): Behavior[Responses.Result] = message match {
    case SetResult(key:String,value:String)=>{

      context.log.info("value of key " + key +" was set "+value)
      Behaviors.same
    }
    case GetResult(key: String, value: String) => {
      value match {
        case "None" =>
          context.log.info("value of key " + key + " not found")
        case _ =>
          context.log.info("value of key " + key + " is " + value)      }

      Behaviors.same
    }
    case _ => {
      context.log.info("not yet implemented")
      Behaviors.same
    }
  }
}

