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

      //val client = context.spawnAnonymous(Client())
      context.system.receptionist ! Receptionist.Subscribe(Client.clientServiceKey, context.self)
      context.system.receptionist ! Receptionist.Subscribe(Store.storeServiceKey, context.self)
      context.system.receptionist ! Receptionist.Subscribe(FileReader.serviceKey, context.self)

      Behaviors.same
     //Behaviors.receiveMessagePartial[Listing] {

     //  case Store.storeServiceKey.Listing(listings) =>
     //    listings.size match {
     //      case 0 =>
     //      case _ =>
     //        println("store creation detected")
     //        //listings.foreach(storeRef => context.spawnAnonymous(Client(storeRef)))
     //    }
     //    Behaviors.same


     //  case Client.clientServiceKey.Listing(listings) =>
     //    listings.size match {
     //      case 0 =>
     //      case _ =>
     //        println("client creation detected")
     //       //val fileReader = context.spawnAnonymous(FileReader())
     //       //listings.foreach(client =>
     //       //  fileReader ! FileReader.File("./trip_data_100.csv", client)
     //       //)
     //    }
     //    Behaviors.same

     //  case FileReader.serviceKey.Listing(listings) =>
     //    listings.size match {
     //      case 0 =>
     //      case _ =>
     //        println("fileReader creation detected")
     //    }
     //    Behaviors.same

     //  case CreateManager.serviceKey.Listing(listings) =>
     //    println("manager creation detected")
     //    Behaviors.same
     //}


  }
  /*
  def apply(): Behavior[Listing] = Behaviors.setup[Listing] { // bekommt keine
    println("in here 0")

    Behaviors.setup[Listing] { context =>
      println("in here 2")
      //val createManager = context.spawn(CreateManager(),"the_createManager")

      context.system.receptionist ! Receptionist.Subscribe(Client.clientServiceKey, context.self)
      context.system.receptionist ! Receptionist.Subscribe(Store.storeServiceKey, context.self)
      val store = context.spawn(Store(), "the-store")
      val client1 = context.spawn(Client(store), "client1")



      Behaviors.receiveMessagePartial[Listing] {
        case Store.storeServiceKey.Listing(listings) =>
          print("store creation detected")
          listings.foreach(ps => context.spawnAnonymous(Client(ps)))
          Behaviors.same
      }
      scala.io.StdIn.readLine()
      Behaviors.same
      /*
      val store = context.spawn(Store(), "the-store")
      context.log.info("logging some stuff")
      val client1 = context.spawn(Client(store), "client1")
      val client2 = context.spawn(Client(store), "client2")
      createManager ! CreateManager.CreateClient("client3")
      createManager ! CreateManager.CreateClient("client4")
      createManager ! CreateManager.CreateClient("client5")
      scala.io.StdIn.readLine()


      val reader = context.spawn(FileReader(), "reader")
      val filename = "./trip_data_100.csv"
      reader ! FileReader.File(filename, client1)

      scala.io.StdIn.readLine()
      client1 ! Client.Count()
      Behaviors.same
    */
    }

  } */
}

