
import akka.actor.typed.receptionist.Receptionist.{Listing, listing}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, MailboxSelector}
import akka.actor.typed.scaladsl.Behaviors

object MainGrpdClientGuard {

  def apply(): Behavior[Listing] = Behaviors.setup[Listing] {
    context =>
      val client =context.spawnAnonymous(GrcpClient(50051, "localhost"))
      StoreShard.initSharding(context.system)
      client ! (GrcpClient.Set("myKey", "Myvalue"))
      Behaviors.empty
  }
  }
