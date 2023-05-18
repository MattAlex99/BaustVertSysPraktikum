package storeGRCP

import de.hfu.protos.messages._
import io.grpc.{ManagedChannel, ManagedChannelBuilder}

import java.util.logging.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}





class  GrcpClient  (port:Int,host:String){


  val logger = Logger.getLogger(this.getClass.getName)
  val channel: ManagedChannel = ManagedChannelBuilder
    .forAddress(host, port)
    .usePlaintext()
    .asInstanceOf[ManagedChannelBuilder[_]]
    .build()
  logger.info("opening channel on "+host+":"+port)

  def print_set_reply(response:Try[SetReply])={
    logger.info("may have recived a response:")
    response match {
      case Success(succ_response) => logger.info("recieved set response with key: " + succ_response.key+" value: "+succ_response.value)
      case Failure(exception: Exception) => exception.printStackTrace()
    }
  }

  def print_get_reply(response: Try[GetReply]) = {
    logger.info("may have recived a get response:")
    response match {
      case Success(succ_response) =>
        succ_response.value match {
          case Some(response_key) =>
            logger.info ("recieved get response with key: " + succ_response.key + " value: " + response_key)
          case None =>
            logger.info("No entry for key "+ succ_response.key + "was found")
        }
          case Failure(exception: Exception) => exception.printStackTrace()
    }
  }

    def getKV(key: String) = {
      val asynch_request = GetRequest(key)
      logger.info("try to Get key " + key)
      try {
        val reply = GrpcClientGrpc
          .stub(channel)
          .get(asynch_request)
          .onComplete(print_get_reply) //implicit context oben importiert (import scala.concurrent.ExecutionContext.Implicits.global)
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }

    def setKV(key:String, value:String) = {
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
    }
}


