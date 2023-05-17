package akkaStore

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akkaStore.Responses.Result

object StoreShard {
  sealed trait Message

  case class Get(replyTo: ActorRef[Result], key: Seq[Byte]) extends Message
  case class Set(replyTo: ActorRef[Result], key: Seq[Byte], value: Seq[Byte]) extends Message

  case class SetBatch(pairs: List[(Seq[Byte],Seq[Byte],ActorRef[Result])]) extends Message

  case class PrintInfo() extends Message

  val TypeKey: EntityTypeKey[StoreShard.Message] =EntityTypeKey[StoreShard.Message]("akkaStore.StoreShard")



    def initSharding(system: ActorSystem[_]): Unit = {
    println("initiating store shards")
    val sharding = ClusterSharding(system)
    sharding.init(Entity(TypeKey) { entityContext =>
      StoreShard(entityContext.entityId)
    })

    }

  def apply(shard_id:String): Behavior[Message] = {
    Behaviors.setup { context =>
      println("creating akkaStore.StoreShard with id: ", shard_id)
      new StoreShard(context,shard_id)
    }
  }
}

class  StoreShard(context: ActorContext[StoreShard.Message], shard_id:String)extends AbstractBehavior[StoreShard.Message](context) {
  import Responses.{GetResultSuccessful, SetResult}
  import StoreShard._
  val storedData: scala.collection.mutable.Map[Seq[Byte],Seq[Byte]] = scala.collection.mutable.Map.empty[Seq[Byte],Seq[Byte]]

  override def onMessage(message: Message): Behavior[Message] = message match {
    case Get(replyTo: ActorRef[Result], key: Seq[Byte]) => {
      //get the string from the map and create a option [String] Object
      val value = storedData.get(key)
      replyTo ! GetResultSuccessful(key, value)
      Behaviors.same
    }
    case PrintInfo()=>{
      println("ShardStore with ID: "+shard_id+"  has "+storedData.size+" stored items")
      Behaviors.same
    }
    case  SetBatch(pairs: List[(Seq[Byte],Seq[Byte],ActorRef[Result])]) =>{
      pairs.foreach(pair=>{
        val key=pair._1
        val value=pair._2
        val replyTo = pair._3
        storedData.put(key, value)
        replyTo ! SetResult(key, value)
        })
      Behaviors.same
    }
    case Set(replyTo: ActorRef[Result], key: Seq[Byte], value: Seq[Byte]) => {
      //hier ganzes batch senden
      storedData.put(key, value)
      replyTo ! SetResult(key, value)
      Behaviors.same
    }

    case _ => {
      context.log.info("Faulty Message (to akkaStore.Client)")
      context.log.info(message.toString)
      Behaviors.same
    }

  }
}
