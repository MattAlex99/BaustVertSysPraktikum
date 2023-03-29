import akka.NotUsed
import akka.actor.TypedActor.context
import akka.actor.typed.scaladsl.adapter.ClassicActorContextOps
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior} // immer darauf achten, dass "typed" dransteht





object Main extends App {
  val guard = ActorSystem(Guard(), "guard")
//  println("Starting ...")
//
//  val system = ActorSystem(Guard(), "hfu")
//  // Läuft unendlich lang
//  println("Press any key to terminate ...")
//  Console.in.read()
//  system.terminate()
//  println("Terminating ...") // Abarbeitungsreihenfolge ist anders als erwartet
}