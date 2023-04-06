import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import java.io.File
import java.util.Scanner
object FileReader {
  sealed trait Message
  case class File(filename: String, client: ActorRef[Client.Command]) extends Message

  def apply(): Behavior[Message] = {
    Behaviors.setup { context =>
      //store: ActorRef[Store.Command]
      new FileReader(context)
    }
  }
}


class FileReader (context: ActorContext[FileReader.Message])
  extends AbstractBehavior[FileReader.Message](context) {

  //import FileReader._
  import FileReader.Message
  import Client._
  override def onMessage(message: Message): Behavior[Message] = message match {
    case FileReader.File(filename: String, client: ActorRef[Client.Command]) => {
      context.log.info("Reading Files by Line")
      //Prepare a scanner to read line by line
      val file = new File(filename)
      val reader = new Scanner(file)
      while (reader.hasNextLine) {
        //Process every read line
        val line = reader.nextLine()
        val values = line.split(",").map(_.trim)
        client ! Client.Set(values(0), values(1))
        println("Press enter to process the next entry")
        scala.io.StdIn.readLine()
        //println(line)
      }
      Behaviors.same
    }
    case _ => {
      context.log.info("Unexpected Message received")
      Behaviors.same

    }
  }
}
