package storeGRCP
import com.typesafe.config.{Config, ConfigFactory}

import akka.actor.typed.ActorSystem

object MainGrcpServer extends App{

  val newSystem =ActorSystem(GrcpServer(50051,"localhost"), "hfu-grcp")

}
