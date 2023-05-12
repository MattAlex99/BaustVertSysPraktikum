package storeGRCP

import akka.actor.typed.ActorSystem

object MainGrcpClient  extends App{
  val clientSystem =ActorSystem(GrcpClient(50051,"localhost"), "hfu-grcp")
  clientSystem ! (GrcpClient.Set("myKey","Myvalue"))
  Thread.sleep(1000000)
}
