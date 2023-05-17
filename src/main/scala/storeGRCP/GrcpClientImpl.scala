package storeGRCP

import akkaStore.Responses.SetResult
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akkaStore.{Responses, Utils}
import de.hfu.protos.messages._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}


class GrcpClientImpl(server:GrcpServer,serverRef:ActorRef[GrcpServer.ServerCommand], context:ActorContext[GrcpServer.ServerCommand]) extends GrpcClientGrpc.GrpcClient {

  import akka.util.Timeout

  // asking someone requires a timeout if the timeout hits without response
  // the ask is failed with a TimeoutException
  implicit val timeout: Timeout = 3.seconds
  implicit val scheduler = context.system.scheduler

  override def get(request: GetRequest): Future[GetReply] = {

    val serverResponse: Future[Responses.Result] = server.getKV(request.key)
    val serverResponse_mapped: Future[Responses.GetResultSuccessful] = serverResponse.map(a => a.asInstanceOf[Responses.GetResultSuccessful])
    println("")
    var res = new GetReply("errorK",Some("errorV"))

    //TODO Problem hier ist, dass alle unterhalb 2 mal ausgeführt wird
    // on complete scheint nicht wirklich zu warten, bis ein ergebnis da ist.
    // wenn der sleep unten fehlt wird der default value (res) zurück gegeben, obwohl  der Success case unten durhcloffen wird.
    serverResponse_mapped.onComplete {
      case Success(any_response: Any) =>
        val response = any_response.asInstanceOf[Responses.GetResultSuccessful]
        println("correct case:")
         response.value match {
          case Some (read_value) =>
            val result = new GetReply(Utils.customByteToString(response.key), Some(Utils.customByteToString(response.value.get)))
            res = result
            println(result)
            //this value below is never returned, even if the libnes above arre executed
            return Future.successful(result)
          case _ =>
            val result = new GetReply(Utils.customByteToString(response.key), None)
            res = result
            println(result)
            //this value below is never returned, even if the libnes above arre executed
            return Future.successful(result)
        }
        val result = new GetReply(Utils.customByteToString(response.key),Some(Utils.customByteToString(response.value.get)))
        res = result
        println(result)
        //this value below is never returned, even if the libnes above arre executed
        return Future.successful(result)

      //return Future.successful(result)
      case Failure(exception) => exception.printStackTrace()
        return Future.failed(exception)
    }
    Thread.sleep(10000)
    println("res before:", res)
    //return Future.failed(new Exception("mayor error, while waiting for responese"))
    return Future.successful(res)
  }

  override def set(request: SetRequest): Future[SetReply] = {
    println("recieved Set Command with:")
    println("  "+request.key+" "+request.value)
    //serverRef ! storeGRCP.GrcpServer.Set(request.key,request.value)
    //XXX


    //val result: Future[akkaStore.Responses.Result] = serverRef ? (ref => storeGRCP.GrcpServer.Set(request.key,request.value,ref))
    val serverResponse: Future[Responses.Result] = server.Setkv(request.key, request.value)
    val serverResponse_mapped:Future[Responses.SetResult]=serverResponse.map(a=> a.asInstanceOf[Responses.SetResult])
    println("")
    var res= new SetReply("errorK","errorV")

    //TODO Problem hier ist, dass alle unterhalb 2 mal ausgeführt wird
    // on complete scheint nicht wirklich zu warten, bis ein ergebnis da ist.
    // wenn der sleep unten fehlt wird der default value (res) zurück gegeben, obwohl  der Success case unten durhcloffen wird.
    serverResponse_mapped.onComplete{
      case Success(any_response:Any) =>
        val response= any_response.asInstanceOf[Responses.SetResult]
        println("correct case:")
        val result= new SetReply(Utils.customByteToString(response.key),Utils.customByteToString(response.value))
        res = result
        println(result)
        //this value below is never returned, even if the libnes above arre executed
       return Future.successful(result)

      //return Future.successful(result)
      case Failure(exception) => exception.printStackTrace()
        return Future.failed(exception)
    }
    Thread.sleep(10000)
    println("res before:", res)
    //return Future.failed(new Exception("mayor error, while waiting for responese"))
    return Future.successful(res)
  }
}



