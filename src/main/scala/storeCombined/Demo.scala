package storeCombined

class Demo(client: StoreClient) {
  private def executeAction(key: String)(value: Option[String]): Unit =
    value match {
      case Some(value) => println(s"found value $value for key $key")
      case None => println(s"did not find value for $key")
    }
  private def get(key: String): Unit = {
    client.get(key, executeAction(key))
  }



  client.set("DE", "Germany")
  get("DE")
  get("UK")
  Console.in.read()
}