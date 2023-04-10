import Store.Count
import akka.NotUsed
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.Receptionist.{Listing, listing}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object Guard {
  def apply(): Behavior[Listing] = Behaviors.setup[Listing] {
    context =>
      context.spawnAnonymous(FileReaderStarter("FileReader1_"))

      scala.io.StdIn.readLine()

      context.spawnAnonymous(ClientStarter("clientBatch1_"))
      scala.io.StdIn.readLine()
      context.spawnAnonymous(Store())

      context.system.receptionist ! Receptionist.Subscribe(Client.clientServiceKey, context.self)
      context.system.receptionist ! Receptionist.Subscribe(Store.storeServiceKey, context.self)
      context.system.receptionist ! Receptionist.Subscribe(FileReader.serviceKey, context.self)

      Behaviors.same
  }
}

