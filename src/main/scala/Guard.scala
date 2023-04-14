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
      println("   all= Alle 3 (nur von Port 25254 möglich)")


      val inputValue= scala.io.StdIn.readLine()

      inputValue match {
        case "1" =>
          val configuration = Utils.createConfiguration(25251)
          //case "2" => context.spawnAnonymous(ClientStarter("clientBatch1_"))
        case "2" => context.spawn(Client(),"initialClient")
        //case "3" => context.spawnAnonymous(FileReaderStarter("FileReader1_"))
        case "3" => context.spawn(FileReader(),"initialFilereader")
        case "all" =>
          val configuration1 = Utils.createConfiguration(25251)
          val client= ActorSystem(Client(), "hfu", configuration1)
          val configuration2 = Utils.createConfiguration(25252)
          ActorSystem(Store(), "hfu", configuration2)
          val configuration3 = Utils.createConfiguration(25253)
          ActorSystem(FileReader(), "hfu", configuration3)

        case _ => println("fehlerhafte eingabe ")
      }


      Behaviors.same
  }
}

