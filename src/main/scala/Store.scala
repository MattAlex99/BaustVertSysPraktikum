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

  case class SetBatch(responeActor:ActorRef[Result],pairs: List[(Seq[Byte],Seq[Byte])]) extends Command

  val storeServiceKey: ServiceKey[Command] = ServiceKey[Command]("StoreService")


  def apply(): Behavior[Command] = {
    Behaviors.setup { context =>

      //MinimalShard.initSharding(context.system)
      val local_shard=context.spawnAnonymous(MinimalShard("5"))
      context.system.receptionist ! Receptionist.Register(Store.storeServiceKey, context.self)
      println("Creating Store")
      new Store(context,local_shard)
    }
  }
}

class Store private (context: ActorContext[Store.Command],local_shard:ActorRef[MinimalShard.Message])extends AbstractBehavior[Store.Command](context) {
  val storedData: scala.collection.mutable.Map[Seq[Byte],Seq[Byte]] = scala.collection.mutable.Map.empty[Seq[Byte],Seq[Byte]]
  import Store._
  val actorSystem: ActorSystem[Nothing] =context.system
  val sharding: ClusterSharding = ClusterSharding(actorSystem)
  val numOfUsedShards=4

  def getShardID(key:Seq[Byte]):String={
    val hash=key.hashCode()
    val result_int= abs(hash)% numOfUsedShards
    return result_int.toString
  }
  override def onMessage(message: Command): Behavior[Command] = message match {

    //Processing Get requests
    case Get(replyTo: ActorRef[Result], key: Seq[Byte]) => {
      //TODO make sure program can deal with empty respones
      val shardId=getShardID(key)
      val ref = sharding.entityRefFor(MinimalShard.TypeKey, shardId)
      ref ! MinimalShard.Get(replyTo,key)

      //get the string from the map and create a option [String] Object
      //val value = storedData.get(key)
      //val valueString: Option[String] = value match {
      //  case Some(arr) => Some(new String(arr.toArray, "UTF-8"))
      //  case None => None
      //}
      //val keyString=new String(key.toArray, StandardCharsets.UTF_8)
      //replyTo ! GetResultSuccessful(keyString, valueString)
      Behaviors.same
    }

    case Count(replyTo: ActorRef[Result]) => {
      //count the number of stored elements
      val result = storedData.size
      replyTo ! CountResult(result)
      Behaviors.same
    }

    case SetBatch(responeActor:ActorRef[Responses.Result],pairs: List[(Seq[Byte],Seq[Byte])])=>{
      pairs.foreach(kv=>{
        val key= kv._1
        val value= kv._2
        val shardId = getShardID(key)
        println("geting shard with id:",shardId)
        val ref = sharding.entityRefFor(MinimalShard.TypeKey, shardId)
        println("shard:",ref," id:",ref.entityId)
        ref ! MinimalShard.Set(responeActor, key, value)
      })
      //val stringPairs = pairs.map(kv=> (custonByteToString(kv._1),custonByteToString(kv._2)))
      //responeActor ! Responses.SetResponseBatch(stringPairs)
      Behaviors.same
    }

    case Set(replyTo: ActorRef[Result], key: Seq[Byte], value: Seq[Byte]) => {
      print("setting: ",key)
      val shardId = getShardID(key)
     //val ref = sharding.entityRefFor(StoreShard.TypeKey, shardId)
     //ref ! StoreShard.Set(replyTo, key,value)

      ////Put the received value as the new one
      //storedData.put(key,value)
      //val keyString=key.toString()
      //val valueString= value.toString()
      //replyTo ! SetResult(keyString,valueString)
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

