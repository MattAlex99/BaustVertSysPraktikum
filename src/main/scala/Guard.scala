import Store.Count
import akka.NotUsed
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.Receptionist.{Listing, listing}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object Guard {
  def apply(): Behavior[Listing] = Behaviors.setup[Listing] {
    context =>


      println("What Do you want to start?")
      println("   1=Server")
      println("   2=Client")
      println("   3=FileReader")
      println("   all= Alle 3")


      val inputValue= scala.io.StdIn.readLine()

      inputValue match {
        case "1" => context.spawn(Store(),"initialStore")
        //case "2" => context.spawnAnonymous(ClientStarter("clientBatch1_"))
        case "2" => context.spawn(Client(),"initialClient")
        //case "3" => context.spawnAnonymous(FileReaderStarter("FileReader1_"))
        case "3" => context.spawn(FileReader(),"initialFilereader")
        case "all" =>
          context.spawn(Store(),"initialStore")
          context.spawn(Client(),"initialClient")
          context.spawn(FileReader(),"initialFilereader")
          //context.spawnAnonymous(FileReaderStarter("FileReader1_"))
        case _ => println("fehlerhafte eingabe ")
      }


      //context.system.receptionist ! Receptionist.Subscribe(Client.clientServiceKey, context.self)
      //context.system.receptionist ! Receptionist.Subscribe(Store.storeServiceKey, context.self)
      //context.system.receptionist ! Receptionist.Subscribe(FileReader.serviceKey, context.self)

      Behaviors.same
  }
}

