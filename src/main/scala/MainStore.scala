import akka.actor.typed.ActorSystem
object MainStore extends App{
  val configuration = Utils.createConfiguration(25253)

  val newSystem =ActorSystem(Store(), "hfu",configuration)
  StoreShard.initSharding(newSystem)

}
