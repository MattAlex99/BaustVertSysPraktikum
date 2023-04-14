import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import java.io.File
import java.util.Scanner
import scala.annotation.tailrec
object FileReader {
  sealed trait Message
  case class File(filename: String, client: ActorRef[Client.Command]) extends Message
  val serviceKey: ServiceKey[Message] = ServiceKey[Message]("clientService")
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
  val batch_size=4000
  override def onMessage(message: Message): Behavior[Message] = message match {
    case FileReader.File(filename: String, client: ActorRef[Client.Command]) => {
      context.log.info("Reading Files by Line")
      //Prepare a scanner to read line by line
      val file = new File(filename)
      val reader = new Scanner(file)
      while (reader.hasNextLine) {
        //Process every read line
        val currentBatch= getNextBatch(0,batch_size,List[(String,String)](),reader)
        //println("batch",currentBatch)
        client ! Client.Set(currentBatch)
      }
      Thread.sleep(12000)
      context.log.info("sending count request")
      client ! Client.Count()
      Behaviors.stopped
    }
    case ListingResponse(listing) => {
      //spawn one reader and make it send messages to every client
      val clients = listing.serviceInstances(Client.clientServiceKey)
      clients.size match {
        case 0 =>
        case _  =>
          println("Starting Reading Process")
          clients.foreach(client => println(client))
          clients.foreach(client => context.self ! FileReader.File("./trip_data_100.csv", client))
      }

      Behaviors.same
    }
    case _ => {
      context.log.info("Unexpected Message received")
      Behaviors.stopped

    }
  }

  @tailrec
  final def getNextBatch(currentCount: Integer, batchSize: Integer, currentValues: List[(String,String)], scanner: Scanner): List[(String,String)] = {
    //TODO ersetzen mit den batch read
    // file reader ist fromFile
    // batch lesen mit
    if (scanner.hasNextLine) {
      if (currentCount == batchSize)
        return currentValues
      else {
        val nextLine=scanner.nextLine()
        val splitValues=nextLine.split(",")
        val newValue= (splitValues(0),splitValues(1))
        return getNextBatch(currentCount + 1,
          batchSize,
          currentValues++List(newValue),
          scanner)
      }
    } else {
      return currentValues
    }

  }

}
