import akka.NotUsed
import akka.actor.TypedActor.context
import akka.actor.typed.scaladsl.adapter.ClassicActorContextOps
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior}

import java.time.InstantSource.system // immer darauf achten, dass "typed" dransteht




object Main {
  def main(args: Array[String]): Unit = {
    val configuration = Utils.createConfiguration(args(0).toInt)
    val guard = ActorSystem(Guard(), "hfu", configuration)
  }


}