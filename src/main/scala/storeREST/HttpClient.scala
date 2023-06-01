package storeREST
import spray.json._

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import storeCombined.StoreClient
import akka.actor.ActorSystem
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.{Level, Logger}
import spray.json.JsValue

import scala.concurrent.Future
import sttp.client3._
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

class HttpClient(url:String, port:String) extends StoreClient{

  //blende unnötige Log ausgaben aus
  val defaultChannelPoolLogger: Logger = LoggerFactory.getLogger("org.asynchttpclient.netty.channel.DefaultChannelPool").asInstanceOf[Logger]
  defaultChannelPoolLogger.setLevel(Level.WARN)

  def get(key: String, action: Option[String] => Unit): Unit ={

    val getUrl = s"http://$url:$port/get/$key"
    val response: Future[requests.Response] = Future {
      val a = requests.get(getUrl, check = false)
      a
    }

    response.onComplete {
      case Success(message) =>
        message.statusCode match{
          case 404 =>
            action(None)
          case 200 =>
            val json: JsValue = message.data.toString().parseJson
            val jsObject: JsObject = json.asJsObject
            val map: Map[String, JsValue] = jsObject.fields
            action(Some(map("value").toString()))
          case _ =>
        }
    }

  }


  def set(key: String, value: String): Unit ={
    val address = "http://"+url +":"+port+"/set"
    println(address)
    val postString = "{\"key\":\"" +key+ "\",\"value\":\""+ value+"\"}"
    val response = requests.post(address, data = postString, headers = Map("Content-Type" -> "application/json"))
  }

}
