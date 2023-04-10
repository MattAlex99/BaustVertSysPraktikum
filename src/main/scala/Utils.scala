import com.typesafe.config.{Config, ConfigFactory}


object Utils {
  def createConfiguration(port:Int):Config= ConfigFactory
    .parseString(s"akka.remote.artery.canonical.port=$port")
    .withFallback(ConfigFactory.load())
}
