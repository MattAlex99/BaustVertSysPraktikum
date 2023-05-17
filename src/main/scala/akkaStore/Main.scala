package akkaStore

import akka.actor.typed.ActorSystem

object Main {
  def main(args: Array[String]): Unit = {
    val configuration = Utils.createConfiguration(args(0).toInt)
    val guard = ActorSystem(Guard(), "hfu", configuration)
  }


}
