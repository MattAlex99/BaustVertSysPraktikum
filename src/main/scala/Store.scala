import Responses.{CountResult, GetResultSuccessful, Result, SetResult}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import java.nio.charset.StandardCharsets

object Store{
  sealed trait Command
  case class Get(replyTo: ActorRef[Result], key: Seq[Byte]) extends Command
  case class Set(replyTo: ActorRef[Result], key: Seq[Byte], value: Seq[Byte]) extends Command
  case class Count(replyTo: ActorRef[Result]) extends Command
  private case class ListingResponse(listing: Receptionist.Listing) extends Command

  val storeServiceKey: ServiceKey[Command] = ServiceKey[Command]("StoreService")


  def apply(): Behavior[Command] = {
    Behaviors.setup { context =>
      val listingResponseAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse.apply)
      context.system.receptionist ! Receptionist.Register(Store.storeServiceKey, context.self)
      context.log.info("Store registered")
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

    case Count(replyTo: ActorRef[Result]) => {
      //count the number of stored elements
      val result = storedData.size
      replyTo ! CountResult(result)
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
      context.log.info("Unexpected Message received (by Store)")
      this
    }
  }




}

