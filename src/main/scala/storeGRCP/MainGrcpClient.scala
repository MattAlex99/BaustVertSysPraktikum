package storeGRCP

object MainGrcpClient  extends App{
  //val configuration = Utils.createConfiguration(25252)
  //val clientSystem =ActorSystem(storeGRCP.GrcpClient(50051,"localhost"), "hfu",configuration)
  //akkaStore.StoreShard.initSharding(clientSystem)
  //clientSystem ! (storeGRCP.GrcpClient.Set("myKey","Myvalue"))

  val client = new GrcpClient(50051,"localhost")
  client.setKV("myKey","myValue")
  scala.io.StdIn.readLine()
  client.getKV("myKey")
  client.getKV("yourKey")
  Thread.sleep(1000000)
}

