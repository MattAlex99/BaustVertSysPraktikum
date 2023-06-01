package storeGRCP

object MainGrcpClient  extends App{

  val client = new GrcpClient(50051,"localhost")
  scala.io.StdIn.readLine()
  client.set("myKey","myValue")
  scala.io.StdIn.readLine()
  client.getDirect("myKey")
  client.getDirect("yourKey")
  Thread.sleep(1000000)
}

