package storeGRCP

object MainGrcpClient  extends App{

  val client = new GrcpClient(50051,"localhost")
  client.setKV("myKey","myValue")
  scala.io.StdIn.readLine()
  client.getKV("myKey")
  client.getKV("yourKey")
  Thread.sleep(1000000)
}

