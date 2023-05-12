package storeGRCP

package storeGRCP


import akka.actor.typed.ActorRef
import de.hfu.protos.messages.{GetReply, GetRequest, GrpcClientGrpc, SetReply, SetRequest}

import scala.concurrent.Future


class GrcpClientImpl(server:GrcpServer) extends GrpcClientGrpc.GrpcClient {

  override def get(request: GetRequest): Future[GetReply] = {
    //pass key on for now
    println("passing key on")
    val result = request.key
    val reply = GetReply(result)
    Future.successful(reply)
  }

  override def set(request: SetRequest): Future[SetReply] = {
    println("recieved set with:")
    println("  "+request.key+" "+request.value)
    server.setKey(request.key,request.value)

    //dummies below
    val result= new SetReply(request.key,request.value)
    Future.successful(result)
  }
}
