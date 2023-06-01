package storeCombined

package storeCombined

import akka.actor.typed.ActorSystem
import akkaStore.{Store, StoreShard, Utils}
import storeGRCP.GrcpClient
import storeREST.{HttpClient, HttpServerActor}

object storeCombinedHttpMain extends App {
  //starte den Store
  val configurationStore = Utils.createConfiguration(25253)
  val storeSystem = ActorSystem(Store(), "hfu", configurationStore)
  StoreShard.initSharding(storeSystem)

  //starte GRPC Server
  val configuration = Utils.createConfiguration(25251)
  val newSystem = ActorSystem(HttpServerActor(8080, "localhost"), "hfu", configuration)
  StoreShard.initSharding(newSystem)



  //erstelle GRPC Client
  val client = new HttpClient("localhost","8080")

  //starte Demo
  Thread.sleep(12000)
  val demo = new Demo(client)

}
