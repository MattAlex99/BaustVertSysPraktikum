import akka.NotUsed
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior} // immer darauf achten, dass "typed" dransteht
/*
//// Actor
//// Beschrieben durch Behavior mit Typ der Nachrichten, die der Actor empfängt
//object ToUpper {
//  def apply(): Behavior[String] = {
//    // Wird hier new Behavior[String] mit new erzeugt? Nein, dafür gibt es Factory-Methoden
//    Behaviors.receive{ (context, text) => // Nachrichten empfangen -- text ist String, context hat logger, Ref auf Actor, ...
//      context.log.info("Hello " + text)
//      // println("Hello " + text) Nur für Amateure :-)
//      // mit context.spawn kann man einen anderen Actor starten
//      Behaviors.same // Mache das gleiche für alle anderen Nachrichten ??? Mach so weiter
//    } // nimmt Nachrichten entgegen
//  }
//}

//object ToUpper {
//  def apply(): Behavior[String] = {
//    Behaviors.receive{ (context, text) =>
//      text match {
//        case "End" => {
//          context.log.info(s"Stopping actor ${context.self.path.name}")
//          Behaviors.stopped
//        }
//        case _ => {
//          context.log.info(s"Hello ${text}")
//          Behaviors.same
//        }
//      }
//    }
//  }
//}

// Startet andere Actoren
// Initiale Arbeit findet hier statt
//object Guard {
//  def apply(): Behavior[NotUsed] = { // bekommt keine
//    Behaviors.setup{ context =>
//      val toUpper = context.spawn(ToUpper(), "toUpper")
//      // context.spawnAnonymous erzeugt Actor mit einzigartigem Namen
//      toUpper ! "world"
//      toUpper ! "Alex"
//      toUpper ! "End"
//      val anotherToUpper = context.spawn(ToUpper(), "anotherToUpper")
//      anotherToUpper ! "Donald" // Reihenfolge zwischen Aktoren nicht klar
//      anotherToUpper ! "End"
//      Behaviors.same
//      val anonymousToUpper = context.spawnAnonymous(ToUpper())
//      anonymousToUpper ! "Duck" // Reihenfolge zwischen Aktoren nicht klar
//      anonymousToUpper ! "End"
//      anotherToUpper ! "HURZ" //  Wird nicht funktionieren
//      Behaviors.same
//    }
//  }
//}

object Guard {
  def apply(): Behavior[NotUsed] = { // bekommt keine
    import ToUpper._ // so lokal wie möglich
    Behaviors.setup { context =>
      val toUpper = context.spawn(ToUpper(), "toUpper")
      // context.spawnAnonymous erzeugt Actor mit einzigartigem Namen
      toUpper ! Message("world")
      toUpper ! Message("Alex")
      toUpper ! End
      val anotherToUpper = context.spawn(ToUpper(), "anotherToUpper")
      anotherToUpper ! Message("Donald") // Reihenfolge zwischen Aktoren nicht klar
      anotherToUpper ! End
      Behaviors.same
      val anonymousToUpper = context.spawnAnonymous(ToUpper())
      anonymousToUpper ! Message("Duck") // Reihenfolge zwischen Aktoren nicht klar
      anonymousToUpper ! End
      anotherToUpper ! Message("HURZ") //  Wird nicht funktionieren
      Behaviors.same
    }
  }
}

// Allgemein sind die Nachrichten ein bisschen komplexer: hier eignen sich Case Classes gut
object ToUpper { // Begleiterobjekt um das Protocol zu definieren + apply-Methode

  trait Protocol // oder Command

  case class Message(name: String) extends Protocol

  case object End extends Protocol

  def apply(): Behavior[Protocol] = {
    Behaviors.setup {context => // wir benutzen setup nur weil man einen context braucht
      new ToUpper(context)
    }
  }
}


class ToUpper(context: ActorContext[ToUpper.Protocol]) extends AbstractBehavior[ToUpper.Protocol](context) {
  import ToUpper._
  override def onMessage(message: Protocol): Behavior[Protocol] = message match {
    case End => {
      context.log.info(s"Stopping actor ${context.self.path.name}")
      Behaviors.stopped
    }
    case Message(name) => {
      context.log.info(s"Hello ${name}")
      this
    }
  }
}

object Main extends App {
  println("Starting ...")
  //  val system = ActorSystem(ToUpper(), "hfu") // Actor registrieren
  //  // system ist Stellvertreter des gesamten Actorensystems
  //  system ! "world" // ! ist eine Funktion um Nachrichten zu schicken
  //  system ! "Alex" // in der Mailbox ist die Reihenfolge immer die gleiche
  // Wie startet man einen zweiten Actor?
  // Neues ActorSystem? Nicht klug
  // Stattdessen: Guard startet andere Aktoren
  val system = ActorSystem(Guard(), "hfu")
  // Läuft unendlich lang
  println("Press any key to terminate ...")
  Console.in.read()
  system.terminate()
  println("Terminating ...") // Abarbeitungsreihenfolge ist anders als erwartet
}


 */