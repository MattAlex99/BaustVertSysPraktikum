
import Responses.{GetResult, SetResult}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import java.nio.charset.StandardCharsets
//import akka.util.LineNumbers.Result
import Responses.Result
object Store{
  sealed trait Command
  case class Get(replyTo: ActorRef[Result], key: Seq[Byte]) extends Command
  case class Set(replyTo: ActorRef[Result], key: Seq[Byte], value: Seq[Byte]) extends Command

  def apply(): Behavior[Command] = {
    Behaviors.setup { context =>
      context.log.info("Store created")
      new Store(context)
    }
  }
}

class Store (context: ActorContext[Store.Command])extends AbstractBehavior[Store.Command](context) {
  val storedData: scala.collection.mutable.Map[Seq[Byte],Seq[Byte]] = scala.collection.mutable.Map.empty[Seq[Byte],Seq[Byte]]
  import Store._
  override def onMessage(message: Command): Behavior[Command] = message match {

    //case get
    case Get(replyTo: ActorRef[Result], key: Seq[Byte]) => {
      val value = storedData.get(key)

      val valueString: String = value match {
        case Some(arr) => new String(arr.toArray, "UTF-8")
        case None => ""
      }
      val keyString=new String(key.toArray, StandardCharsets.UTF_8)
      replyTo ! GetResult(keyString, valueString)
      Behaviors.same
    }

    //case Set
    case Set(replyTo: ActorRef[Result], key: Seq[Byte], value: Seq[Byte]) => {
      storedData.put(key,value)
      val valueString = new String(value.toArray, "UTF-8")
      val keyString = new String(key.toArray, StandardCharsets.UTF_8)
      replyTo ! SetResult(keyString,valueString)
      //TODO Wird der Aktor hier beendet
      Behaviors.same
    }

    case _ => {
      context.log.info("got something wrong")
      this
    }
  }




}

