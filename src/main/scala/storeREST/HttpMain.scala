package storeREST
import akka.actor.typed.ActorSystem
import akkaStore.{StoreShard, Utils}


object HttpMain extends App {
  val configuration = Utils.createConfiguration(25251)
  val newSystem = ActorSystem(HttpServerActor(8080, "localhost"), "hfu", configuration)
  StoreShard.initSharding(newSystem)

}
