import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.util.LineNumbers.Result

import java.nio.charset.StandardCharsets

object Responses {
  sealed trait Result

  case class SetResult(key: String, value: String) extends Result
  case class GetResultSuccessful(key: String, value: Option[String]) extends Result



  def apply(): Behavior[Result] = {
    Behaviors.setup { context =>
      new Responses(context)
    }
  }
}


class Responses private (context: ActorContext[Responses.Result]) extends AbstractBehavior[Responses.Result](context) {
  import Responses._
  override def onMessage(message: Responses.Result): Behavior[Responses.Result] = message match {
    case SetResult(key:String,value:String)=>{

      context.log.info("value of key " + key +" was set "+value)
      Behaviors.stopped
    }
    case GetResultSuccessful(key: String, value: Option[String]) => {
      value match {
        case None =>
          context.log.info("value of key " + key + " not found")
        case Some(containedValue) =>
          context.log.info("value of key " + key + " is " + containedValue)      }
      Behaviors.stopped
    }
    case _ => {
      context.log.info("not yet implemented")
      Behaviors.stopped
    }
  }
}

