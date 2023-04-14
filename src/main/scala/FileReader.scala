import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import scala.io.Source
import scala.util.Using
object FileReader {
  sealed trait Message
  case class File(filename: String, client: ActorRef[Client.Command]) extends Message
  val serviceKey: ServiceKey[Message] = ServiceKey[Message]("fileReader")
  private case class ListingResponse(listing: Receptionist.Listing) extends Message

  def apply(): Behavior[Message] = {
    Behaviors.setup { context =>
      print("creating Filereader")
      context.system.receptionist ! Receptionist.Register(serviceKey,context.self)
      val listingResponseAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse.apply)
      context.system.receptionist ! Receptionist.Subscribe(Client.clientServiceKey, listingResponseAdapter)
      new FileReader(context)
    }
  }
}


class FileReader (context: ActorContext[FileReader.Message])
  extends AbstractBehavior[FileReader.Message](context) {

  //import FileReader._
  import FileReader.Message
  import FileReader.ListingResponse
  import Client._
  val batch_size=2000
  override def onMessage(message: Message): Behavior[Message] = message match {
    case FileReader.File(filename: String, client: ActorRef[Client.Command]) => {
      context.log.info("Reading Files by Line")
      Using(Source.fromFile(filename)){ reader =>
        reader.getLines()
          .map(line => (line.split(",")(0), line.split(",")(1)))
          .grouped(batch_size)
          .buffered
          .foreach(batch =>  client ! Client.Set(batch.toList))
        //.foreach(batch =>client ! Client.Set(batch.toList))
      }.get
      println("press enter to get the count")
      scala.io.StdIn.readLine()
      client ! Client.Count()
      Behaviors.same
    }
    case ListingResponse(listing) => {
      //spawn one reader and make it send messages to every client
      val clients = listing.serviceInstances(Client.clientServiceKey)
      clients.size match {
        case 0 =>
        case _  =>
          println("Starting Reading Process")
          clients.foreach(client => context.self ! FileReader.File("../trip_data_1000_000.csv", client))
      }

      Behaviors.same
    }
    case _ => {
      context.log.info("Unexpected Message received")
      Behaviors.stopped

    }
  }

    def SetAndWait(client:ActorRef[Command],batch:Seq[(String,String)]): Unit ={
      client ! Client.Set(batch.toList)
      //Thread.sleep(150)
    }

}
