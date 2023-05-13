import akka.actor.typed.ActorSystem

object MainGrcpClient  extends App{
  val configuration = Utils.createConfiguration(25252)
  val clientSystem =ActorSystem(GrcpClient(50051,"localhost"), "hfu",configuration)
  StoreShard.initSharding(clientSystem)
  clientSystem ! (GrcpClient.Set("myKey","Myvalue"))
  Thread.sleep(1000000)
}

