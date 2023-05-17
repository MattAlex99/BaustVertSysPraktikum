package storeGRCP
import akka.actor.typed.ActorSystem
import akkaStore.{Store, StoreShard, Utils}

object MainStore extends App{
  val configuration = Utils.createConfiguration(25253)
  val newSystem =ActorSystem(Store(), "hfu",configuration)
  StoreShard.initSharding(newSystem)

}
