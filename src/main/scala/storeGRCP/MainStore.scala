package storeGRCP

import akka.actor.typed.ActorSystem
import _root_.Store
object MainStore extends App{
  val newSystem =ActorSystem(Store(), "hfu-grcp")

}
