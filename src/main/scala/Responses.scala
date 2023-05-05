import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import java.nio.charset.StandardCharsets

object Responses {
  sealed trait Result

  case class SetResult(key: Seq[Byte], value: Seq[Byte]) extends Result

  case class SetResponseBatch(kvPairs:List[(String,String)]) extends Result

  case class GetResultSuccessful(key: Seq[Byte], value: Option[Seq[Byte]]) extends Result

  case class CountResult(count:Integer) extends Result


  def apply(): Behavior[Result] = {
    Behaviors.setup { context =>
      new Responses(context)
    }
  }
}


class Responses private (context: ActorContext[Responses.Result]) extends AbstractBehavior[Responses.Result](context) {
  import Responses._

  def printSetResult(key:String,value:String): Unit ={
    context.log.info("value of key " + key +" was set "+value)
  }

  def printSetResult(key: Seq[Byte], value: Seq[Byte]): Unit = {
    context.log.info("value of key " + custonByteToString(key) + " was set " +custonByteToString(value) )
  }

  override def onMessage(message: Responses.Result): Behavior[Responses.Result] = message match {
    case SetResponseBatch(kvPairs: List[(String, String)]) => {
      kvPairs.foreach(pair=>printSetResult(pair._1,pair._2))
      Behaviors.stopped
    }
    case SetResult(key:Seq[Byte],value:Seq[Byte])=>{
      printSetResult(key,value)
      Behaviors.stopped
    }
    case CountResult(count:Integer) => {
      context.log.info("total number of stored keys is "+ count.toString )
      Behaviors.stopped
    }

    case GetResultSuccessful(key: Seq[Byte], value: Option[Seq[Byte]]) => {
      value match {
        case None =>
          context.log.info("value of key " + custonByteToString(key) + " not found")
        case Some(containedValue) =>
          context.log.info("value of key " + custonByteToString(key) + " is " + custonByteToString(containedValue), "UTF-8")
          }
      Behaviors.stopped
    }
    case _ => {
      context.log.info(message.toString)
      context.log.info("Faulty Message (to Responses)")
      Behaviors.stopped
    }
  }

  def custonByteToString(input: Seq[Byte]): String = {
    return new String(input.asInstanceOf[List[Int]].map(_.toByte).toArray, StandardCharsets.UTF_8)
  }
}

