import Responses.{GetResultSuccessful, SetResult}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import java.nio.charset.StandardCharsets
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

class Store private (context: ActorContext[Store.Command])extends AbstractBehavior[Store.Command](context) {
  val storedData: scala.collection.mutable.Map[Seq[Byte],Seq[Byte]] = scala.collection.mutable.Map.empty[Seq[Byte],Seq[Byte]]
  import Store._
  override def onMessage(message: Command): Behavior[Command] = message match {

    //Processing Get requests
    case Get(replyTo: ActorRef[Result], key: Seq[Byte]) => {
      //get the string from the map and create a option [String] Object
      val value = storedData.get(key)
      val valueString: Option[String] = value match {
        case Some(arr) => Some(new String(arr.toArray, "UTF-8"))
        case None => None
      }
      val keyString=new String(key.toArray, StandardCharsets.UTF_8)
      replyTo ! GetResultSuccessful(keyString, valueString)
      Behaviors.same
    }

    //Processing Set Requests
    case Set(replyTo: ActorRef[Result], key: Seq[Byte], value: Seq[Byte]) => {
      //Put the received value as the new one
      storedData.put(key,value)
      val valueString = new String(value.toArray, "UTF-8")
      val keyString = new String(key.toArray, StandardCharsets.UTF_8)
      replyTo ! SetResult(keyString,valueString)
      Behaviors.same
    }

    case _ => {
      context.log.info("Unexpected Message received")
      this
    }
  }




}

