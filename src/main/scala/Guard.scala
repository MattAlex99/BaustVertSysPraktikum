import akka.NotUsed
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object Guard {
  def apply(): Behavior[NotUsed] = { // bekommt keine
    Behaviors.setup { context =>
      val store = context.spawn(Store(), "the-store")
      context.log.info("logging some stuff")
      val client1 = context.spawn(Client(store), "client1")
      val client2 = context.spawn(Client(store), "client2")
      client1 ! Client.Set("IT", "Italia")
      client2 ! Client.Get("IT")
      client1 ! Client.Get("DE")
      client1 ! Client.Get("IT")
      val reader = context.spawn(FileReader(), "reader")
      val filename = "trip_data_1000_000.csv"
      reader ! FileReader.File(filename, client1)


      //val store = ActorSystem(Store(),"mainStore")
      //store.ref
      //val ipaddr = Array[Byte](192.toByte, 168.toByte, 1, 9)
      //store ! (context.self)
      //print("end of guard")
      Behaviors.same
    }
  }
}