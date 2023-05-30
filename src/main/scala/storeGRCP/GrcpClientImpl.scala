package storeGRCP

import akkaStore.Responses.SetResult
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akkaStore.{Responses, Utils}
import de.hfu.protos.messages._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}


class GrcpClientImpl(server:GrcpServer,context:ActorContext[GrcpServer.ServerCommand]) extends GrpcClientGrpc.GrpcClient {

  import akka.util.Timeout

  // asking someone requires a timeout if the timeout hits without response
  // the ask is failed with a TimeoutException
  implicit val timeout: Timeout = 3.seconds
  implicit val scheduler = context.system.scheduler
  override def get(request: GetRequest): Future[GetReply] = {
    val promise = server.getKVFuture(request.key)
    Await.result(promise, 5.seconds)
    promise
  }


  override def set(request: SetRequest): Future[SetReply] = {
    val promise = server.setKVFuture(request.key,request.value)
    Await.result(promise, 5.seconds)
    promise
  }




}



