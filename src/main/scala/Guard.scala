import Store.Count
import akka.NotUsed
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.Receptionist.{Listing, listing}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, MailboxSelector}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding.ShardCommand
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}

object Guard {
  def apply(): Behavior[Listing] = Behaviors.setup[Listing] {
    context =>


      println("What Do you want to start?")
      println("   1=Server")
      println("   2=Client")
      println("   3=FileReader")
      println("   4=execute Tests")
      println("   5= Alle 4 (nur von Port 25254 möglich)")


      val inputValue= scala.io.StdIn.readLine()

      inputValue match {
        case "1" =>context.spawn(Store(),"initialStore")
                    StoreShard.initSharding(context.system)

        case "2" => context.spawn(Client(),"initialClient")
                 StoreShard.initSharding(context.system)

        case "3" => context.spawn(FileReader(5000),"initialFilereader")
                  StoreShard.initSharding(context.system)

        case "4" =>
                  StoreShard.initSharding(context.system)
                  context.spawnAnonymous(ObserverAndExecutor())
        case "5" =>
                  StoreShard.initSharding(context.system)

          val configuration1 = Utils.createConfiguration(25251)
          val ClientSystem=ActorSystem(Client(), "hfu", configuration1)
          StoreShard.initSharding(ClientSystem)


          val configuration2 = Utils.createConfiguration(25252)
          val storeSystem=ActorSystem(Store(), "hfu", configuration2)
          StoreShard.initSharding(storeSystem)

          val configuration3 = Utils.createConfiguration(25253)
          val fileReaderSystem = ActorSystem(FileReader(5000), "hfu", configuration3)
          StoreShard.initSharding(fileReaderSystem)

          println("press enter to execute tests")
          Console.in.readLine()
          context.spawnAnonymous(ObserverAndExecutor())

        case _ => println("fehlerhafte eingabe ")
      }


      Behaviors.same
  }
}

