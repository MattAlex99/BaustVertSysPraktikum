package storeCombined

import akka.actor.typed.ActorSystem
import akkaStore.{Store, StoreShard, Utils}
import storeGRCP.{GrcpClient, GrcpServer}

object storeCombinedGrcpMain extends App {
  //starte den Store
  val configurationStore = Utils.createConfiguration(25253)
  val storeSystem = ActorSystem(Store(), "hfu", configurationStore)
  StoreShard.initSharding(storeSystem)

  //starte GRPC Server
  val configuration = Utils.createConfiguration(25251)
  val newSystem = ActorSystem(GrcpServer(50051, "localhost"), "hfu", configuration)
  StoreShard.initSharding(newSystem)

  //erstelle GRPC Client
  val client = new GrcpClient(50051,"localhost")

  //starte Demo
  val demo = new Demo(client)

}
