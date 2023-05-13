

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, ActorSystem}
import de.hfu.protos.messages._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}
import scala.concurrent.{ExecutionContext, Future}


class GrcpClientImpl(server:GrcpServer,serverRef:ActorRef[GrcpServer.ServerCommand], context:ActorContext[GrcpServer.ServerCommand]) extends GrpcClientGrpc.GrpcClient {

  import akka.actor.typed.scaladsl.AskPattern._
  import akka.util.Timeout

  // asking someone requires a timeout if the timeout hits without response
  // the ask is failed with a TimeoutException
  implicit val timeout: Timeout = 3.seconds
  implicit val scheduler = context.system.scheduler

  // implicit ActorSystem in scope



  // the response callback will be executed on this execution context
  //implicit val ec = system.executionContext

  override def get(request: GetRequest): Future[GetReply] = {
    //pass key on for now
    println("passing key on")
    val result = request.key
    val reply = GetReply(result)
    Future.successful(reply)
  }

  override def set(request: SetRequest): Future[SetReply] = {
    println("recieved Set Command with:")
    println("  "+request.key+" "+request.value)
    //serverRef ! GrcpServer.Set(request.key,request.value)
    //XXX


    //val result: Future[Responses.Result] = serverRef ? (ref => GrcpServer.Set(request.key,request.value,ref))
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
        val result= new SetReply(response.key.toString(),response.value.toString())
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


