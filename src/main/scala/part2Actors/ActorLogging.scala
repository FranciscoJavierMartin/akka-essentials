package part2Actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

object ActorLogging extends App{

  class SimpleActorWithExplicitLogger extends Actor{
    val logger = Logging(context.system,this)
    override def receive: Receive = {
      /*
        1 - Debug
        2 - Info
        3 - Warning/Warn
        4 - Error
       */
      case message => logger.info(message.toString) // Log it
    }
  }

  val system = ActorSystem("LogginDemo")
  val actor = system.actorOf(Props[SimpleActorWithExplicitLogger])

  actor ! "Logging a simple message"

  // #2 - ActorLogging
  class ActorWithLogging extends Actor with ActorLogging{
    override def receive: Receive = {
      case (a,b) => log.info("Two things: {} and {}",a,b)
      case message => log.info(message.toString)
    }
  }

  val simpleActor = system.actorOf(Props[ActorWithLogging])
  simpleActor ! "Logging a simple message by extending a trait"
  simpleActor ! (42,72)
}
