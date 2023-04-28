import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import Responses.{GetResultSuccessful, Result, SetResult}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}

object StoreShard {


  sealed trait Command
  case class Get(replyTo: ActorRef[Result], key: Seq[Byte]) extends Command
  case class Set(replyTo: ActorRef[Result], key: Seq[Byte], value: Seq[Byte]) extends Command

 // val TypeKey: EntityTypeKey[StoreShard.Command] =
 //   EntityTypeKey[StoreShard.Command]("StoreShard")

  def initSharding(system: ActorSystem[_]): Unit = {
    println("initiating store shards")
    //only starts new weather station, if no entity with cntityContext.entityID exists
    //entity content is passed as argument in "sharding.entityRefFor(...)"
    val sharding = ClusterSharding(system)

    //doku
    //sharding.init(
    //  Entity(TypeKey)(createBehavior = entityContext =>
    //    StoreShard(entityContext.entityId)).withRole("StoreShard"))
//
    ////kill weathr
    //ClusterSharding(system).init(Entity(TypeKey) { entityContext =>
    //  StoreShard(entityContext.entityId)
    //})
  }

  def apply(shard_id:String): Behavior[StoreShard.Command] =
    Behaviors.setup { context =>
      println("creating store shard, with id:", shard_id)
    return new StoreShard(context,shard_id)
    }

}


class  StoreShard  (context: ActorContext[StoreShard.Command],shard_id:String)extends AbstractBehavior[StoreShard.Command](context) {
  import StoreShard._
  val storedData: scala.collection.mutable.Map[Seq[Byte],Seq[Byte]] = scala.collection.mutable.Map.empty[Seq[Byte],Seq[Byte]]

  override def onMessage(message: Command): Behavior[Command] = message match {
    case Get(replyTo: ActorRef[Result], key: Seq[Byte]) => {
      //get the string from the map and create a option [String] Object
      val value = storedData.get(key)
      replyTo ! GetResultSuccessful(key, value)
      Behaviors.same
    }

    case Set(replyTo: ActorRef[Result], key: Seq[Byte], value: Seq[Byte]) => {
      //hier ganzes batch senden
      storedData.put(key,value)
      //val sendBatch= currentBatch.map(entry=> (entry._1.getBytes().toSeq,entry._2.getBytes().toSeq))
      replyTo ! SetResult(key,value)
      Behaviors.same
    }

    case _ => {
      context.log.info("Faulty Message (to Client)")
      context.log.info(message.toString)
      Behaviors.same
    }

  }
}

