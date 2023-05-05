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


  def apply(): Behavior[Command] = {
    Behaviors.setup { context =>

      //MinimalShard.initSharding(context.system)
      val local_shard=context.spawnAnonymous(StoreShard("5"))
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
  val numOfUsedShards=10

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
      pairs.foreach(kv=>{
        val key= kv._1
        val value= kv._2
        val responeActor = kv._3
        val shardId = getShardID(key)
        val ref = sharding.entityRefFor(StoreShard.TypeKey, shardId)
        ref ! StoreShard.Set(responeActor, key, value)
      })
      Behaviors.same
    }

    case Set(replyTo: ActorRef[Result], key: Seq[Byte], value: Seq[Byte]) => {
      print("setting: ",key)
      val shardId = getShardID(key)
      val ref = sharding.entityRefFor(StoreShard.TypeKey, shardId)
      ref ! StoreShard.Set(replyTo, key, value)
      Behaviors.same
    }

    case _ => {
      context.log.info("Unexpected Message received (by Store)")
      this
    }
  }


  def custonByteToString(input:Seq[Byte]):String= {
    //Jackson deserialisiert als Seq[Int] ansetelle Seq[Byte], diese Funktion setzt notwendige casts um
    return new String(input.asInstanceOf[List[Int]].map(_.toByte).toArray, StandardCharsets.UTF_8)
  }

}

