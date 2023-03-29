import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import scala.io.BufferedSource
import java.io.File

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

  import FileReader._
  import Client._
  override def onMessage(message: Message): Behavior[Message] = message match {
    case FileReader.File(filename: String, client: ActorRef[Client.Command]) => {
      context.log.info("Reading Files by Line")
      val file = new java.io.File(filename)
      val source: BufferedSource = io.Source.fromFile(file)
      for (line <- source.getLines()) {
        val values = line.split(",").map(_.trim)
        client ! Client.Set(values(0),values(1))
        print("Press enter to process the next entry")
        scala.io.StdIn.readLine()
      }

      source.close()


      Behaviors.same
    }
    case _ => {
      context.log.info("not yet implemented")
      Behaviors.same

    }
  }
}
