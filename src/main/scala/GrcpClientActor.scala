import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import io.grpc.{ManagedChannel, ManagedChannelBuilder}

import java.util.logging.Logger
import scala.util.{Failure, Success, Try}
import de.hfu.protos.messages._

import scala.concurrent.ExecutionContext.Implicits.global



object GrcpClient {
  sealed trait Command
  case class Get(key: String) extends Command
  case class Set(key:String, value:String) extends Command
  case class SetBatch(currentBatch: List[(String, String)]) extends Command

  def apply(port:Int, host:String): Behavior[Command] =
    Behaviors.setup { context =>
      println("client waiting for creation")

      new GrcpClient(context,port,host)
    }

}


class  GrcpClient  (context: ActorContext[GrcpClient.Command],port:Int,host:String)extends AbstractBehavior[GrcpClient.Command](context) {
  import GrcpClient._

  val logger = Logger.getLogger(GrcpClient.getClass.getName)
  val channel: ManagedChannel = ManagedChannelBuilder
    .forAddress(host, port)
    .usePlaintext()
    .asInstanceOf[ManagedChannelBuilder[_]]
    .build()
  logger.info("opening channel on "+host+":"+port)

  def print_set_reply(response:Try[SetReply])={
    logger.info("may have recived a response:")
    response match {
      case Success(succ_response) => logger.info("recieved response " + succ_response)
      case Failure(exception: Exception) => exception.printStackTrace()
    }
  }
  override def onMessage(message: Command): Behavior[Command] = message match {
    case Get(key: String) => {
      print("Not implemented")
      Behaviors.same
    }

    case Set(key:String,value:String) => {
      val asynch_request = SetRequest(key,value)
      logger.info("try to set key " + key + "to value "+ value)
      try {
        val reply = GrpcClientGrpc
          .stub(channel)
          .set(asynch_request)
          .onComplete(print_set_reply) //implicit context oben importiert (import scala.concurrent.ExecutionContext.Implicits.global)
      } catch {
        case e: Exception => e.printStackTrace()
      }
      Behaviors.same
    }


    case _ => {
      context.log.info("Faulty Message (to Client)")
      context.log.info(message.toString)
      Behaviors.same
    }

  }
}


