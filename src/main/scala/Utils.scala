import com.typesafe.config.{Config, ConfigFactory}

import java.nio.charset.StandardCharsets
import scala.util.Success


object Utils {
  def createConfiguration(port:Int):Config= ConfigFactory
    .parseString(s"akka.remote.artery.canonical.port=$port")
    .withFallback(ConfigFactory.load())

  def customByteToString(input: Seq[Byte]): String = {
    return new String(input.asInstanceOf[List[Int]].map(_.toByte).toArray, StandardCharsets.UTF_8)
  }



}
