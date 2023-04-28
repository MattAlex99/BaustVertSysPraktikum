import Store.Count
import akka.NotUsed
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.Receptionist.{Listing, listing}
import akka.actor.typed.{ActorSystem, Behavior, MailboxSelector}
import akka.actor.typed.scaladsl.Behaviors

object Guard {
  def apply(): Behavior[Listing] = Behaviors.setup[Listing] {
    context =>


      println("What Do you want to start?")
      println("   1=Server")
      println("   2=Client")
      println("   3=FileReader")
      println("   4= Alle 3 (nur von Port 25254 möglich)")


      val inputValue= scala.io.StdIn.readLine()

      inputValue match {
        case "1" =>context.spawn(Store(),"initialStore")
        case "2" => context.spawn(Client(),"initialClient")
        case "3" => context.spawn(FileReader(),"initialFilereader")
        case "4" =>
          val configuration1 = Utils.createConfiguration(25251)
          val ClientSystem=ActorSystem(Client(), "hfu", configuration1)
          MinimalShard.initSharding(ClientSystem)

          //context.spawn(StoreShard("1"),"shard_1")
          //context.spawn(StoreShard("2"),"shard_2")
          //context.spawn(StoreShard("3"),"shard_3")
          //context.spawn(StoreShard("4"),"shard_4")
          context.spawnAnonymous(MinimalShard("0"))
          //context.spawnAnonymous(MinimalShard("1"))
          //context.spawnAnonymous(MinimalShard("2"))
          //context.spawnAnonymous(MinimalShard("3"))
          MinimalShard.initSharding(context.system)

          val configuration2 = Utils.createConfiguration(25252)
          val storeSystem=ActorSystem(Store(), "hfu", configuration2)
          MinimalShard.initSharding(storeSystem)

          val configuration3 = Utils.createConfiguration(25253)
          val fileReaderSystem = ActorSystem(FileReader(), "hfu", configuration3)
          MinimalShard.initSharding(fileReaderSystem)

        case "5" =>
          //StoreShard.initSharding(context.system)
          MinimalShard.initSharding(context.system)
          context.spawnAnonymous(MinimalShard("0"))
          context.spawnAnonymous(MinimalShard("1"))
          context.spawnAnonymous(MinimalShard("2"))
          context.spawnAnonymous(MinimalShard("3"))
          context.spawnAnonymous(Store())
          context.spawnAnonymous(Client())
          context.spawnAnonymous(FileReader())


        //context.spawn(StoreShard("1"), "shard_1")
          //context.spawn(StoreShard("2"), "shard_2")
          //context.spawn(StoreShard("3"), "shard_3")
          //context.spawn(StoreShard("0"), "shard_0")
        case _ => println("fehlerhafte eingabe ")
      }


      Behaviors.same
  }
}

