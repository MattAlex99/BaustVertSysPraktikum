import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import Responses.Result
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}

import scala.io.Source
import scala.util.Using

object StoreShard {
  sealed trait Message

  case class Get(replyTo: ActorRef[Result], key: Seq[Byte]) extends Message
  case class Set(replyTo: ActorRef[Result], key: Seq[Byte], value: Seq[Byte]) extends Message

  case class PrintInfo() extends Message

  //TODO Fragen, ob ich hier dieses 1 zu 1 mapping mache, ich glaube das hier ersetzt nur "with Role"
  val TypeKey: EntityTypeKey[StoreShard.Message] =EntityTypeKey[StoreShard.Message]("StoreShard")
  def initSharding(system: ActorSystem[_]): Unit = {
    println("initiating store shards")
    val sharding = ClusterSharding(system)
    sharding.init(Entity(TypeKey) { entityContext =>
      StoreShard(entityContext.entityId)
    })
  }

  def apply(shard_id:String): Behavior[Message] = {
    Behaviors.setup { context =>
      println("creating StoreShard with id: ", shard_id)
      new StoreShard(context,shard_id)
    }
  }
}

class  StoreShard(context: ActorContext[StoreShard.Message], shard_id:String)extends AbstractBehavior[StoreShard.Message](context) {
  import StoreShard._
  import Responses.SetResult
  import Responses.GetResultSuccessful
  val storedData: scala.collection.mutable.Map[Seq[Byte],Seq[Byte]] = scala.collection.mutable.Map.empty[Seq[Byte],Seq[Byte]]

  override def onMessage(message: Message): Behavior[Message] = message match {
    case Get(replyTo: ActorRef[Result], key: Seq[Byte]) => {
      //get the string from the map and create a option [String] Object
      val value = storedData.get(key)
      replyTo ! GetResultSuccessful(key, value)
      Behaviors.same
    }
    case PrintInfo()=>{
      println("ShardStroe with ID: "+shard_id+"  has "+storedData.size+" elements in sytem: "+context.system)
      Behaviors.same
    }
    case Set(replyTo: ActorRef[Result], key: Seq[Byte], value: Seq[Byte]) => {
      //hier ganzes batch senden
      storedData.put(key, value)
      replyTo ! SetResult(key, value)
      Behaviors.same
    }

    case _ => {
      context.log.info("Faulty Message (to Client)")
      context.log.info(message.toString)
      Behaviors.same
    }

  }
}
