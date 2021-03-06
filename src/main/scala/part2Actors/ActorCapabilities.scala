package part2Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App{

  class SimpleActor extends Actor{

    private val Hi: String ="Hi!"

    override def receive: Receive = {
      case this.Hi => sender() ! "Hello, there!" // replying to a message
      case message: String => println(s"[Simple actor] I Have received $message")
      case number: Int => println(s"[Simple actor] I have received a NUMBER: $number")
      case SpecialMessage(contents) => println(s"[Simple actor] I have received something SPECIAL: $contents")
      case SendMessageToYourself(content) => self ! content
      case SayHiTo(ref) => ref ! this.Hi
      // Keep the original sender of the WPM
      case WirelessPhoneMessage(content, ref) => ref forward(content+"s")
    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor],"simpleActor")

  simpleActor ! "hello, actor"

  // 1 - messages can be of any type
  // a) messages must be INMUTABLE
  // b) messages must be SERIALIZABLE
  // in practice use case classes and case objects
  simpleActor ! 42

  case class SpecialMessage(contents: String)
  simpleActor ! SpecialMessage("Some special content")

  // 2 - actors have information about their context and about themselves
  // context.self === `this` in OOP

  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("I am an actor and I am proud of it")

  // 3 - actors can REPLY to messages
  val alice = system.actorOf(Props[SimpleActor],"alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)

  // 4 - dead letters
  alice ! "Hi!" // reply to "me"

  // 5 - forwarding messages
  // D -> A -> B
  // forwarding = sending a message with the original sender

  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("Hi",bob)

  /*
    Exercises

    1. a Counter actor
      - Increment
      - Decrement
      - Print

    2. a Bank account as an actor
      receives
      - Deposit an amount
      - Withdraw an amount
      - Statement

      replies with
      - Success
      - Failure

      interact wit some other kind of actor
   */
}
