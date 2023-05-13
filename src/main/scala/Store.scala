import Responses.{CountResult, GetResultSuccessful, Result, SetResult}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}

import java.nio.charset.StandardCharsets
import scala.math.abs

object Store{
  sealed trait Command
  case class Get(replyTo: ActorRef[Result], key: Seq[Byte]) extends Command
  case class Set(replyTo: ActorRef[Result], key: Seq[Byte], value: Seq[Byte]) extends Command
  case class Count(replyTo: ActorRef[Result]) extends Command

  case class SetBatch(pairs: List[(Seq[Byte],Seq[Byte],ActorRef[Result])]) extends Command

  val storeServiceKey: ServiceKey[Command] = ServiceKey[Command]("StoreService")
  val numOfUsedShards=30

  def apply(): Behavior[Command] = {
    Behaviors.setup { context =>
      context.system.receptionist ! Receptionist.Register(Store.storeServiceKey, context.self)
      println("Creating Store")
      new Store(context)
    }
  }
}

class Store private (context: ActorContext[Store.Command])extends AbstractBehavior[Store.Command](context) {
  val storedData: scala.collection.mutable.Map[Seq[Byte],Seq[Byte]] = scala.collection.mutable.Map.empty[Seq[Byte],Seq[Byte]]
  import Store._
  val actorSystem: ActorSystem[Nothing] =context.system
  val sharding: ClusterSharding = ClusterSharding(actorSystem)


  def getShardID(key:Seq[Byte]):String={
    val hash=key.hashCode()
    val result_int= abs(hash)% numOfUsedShards
    return result_int.toString
  }
  override def onMessage(message: Command): Behavior[Command] = message match {

    //Processing Get requests
    case Get(replyTo: ActorRef[Result], key: Seq[Byte]) => {
      val shardId=getShardID(key)
      val ref = sharding.entityRefFor(StoreShard.TypeKey, shardId)
      ref ! StoreShard.Get(replyTo,key)
      Behaviors.same
    }

    case Count(replyTo: ActorRef[Result]) => {
      //count the number of stored elements
      val result = storedData.size
      replyTo ! CountResult(result)
      Behaviors.same
    }

    case SetBatch(pairs: List[(Seq[Byte],Seq[Byte],ActorRef[Responses.Result])])=>{

      //seperate batch into multiple batches, one for each shard
      val shard_batches =pairs.groupBy(entry=>getShardID(entry._1))
      //send each batch to its respective shard
      shard_batches.foreach(entry=>{
        val shardId=entry._1
        val batch =entry._2
        val shardRef = sharding.entityRefFor(StoreShard.TypeKey, shardId)
        shardRef ! StoreShard.SetBatch(batch)
      })
      Behaviors.same
    }

    case Set(replyTo: ActorRef[Result], key: Seq[Byte], value: Seq[Byte]) => {
      //sets a single key value pair into its respective shardStore
      val shardId = getShardID(key)
      println("setting here",key,value)
      val ref = sharding.entityRefFor(StoreShard.TypeKey, shardId)
      ref ! StoreShard.Set(replyTo, key, value)
      Behaviors.same
    }

    case _ => {
      context.log.info("Unexpected Message received (by Store)")
      this
    }
  }





}

