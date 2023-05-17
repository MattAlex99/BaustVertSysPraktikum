package storeGRCP

import akka.actor.typed.ActorSystem
import akkaStore.{StoreShard, Utils}

object MainGrcpServer extends App{

  val configuration = Utils.createConfiguration(25251)
  val newSystem =ActorSystem(GrcpServer(50051,"localhost"), "hfu",configuration)
  StoreShard.initSharding(newSystem)
  //val newSystem =ActorSystem(akkaStore.Client(), "hfu",configuration)

}


