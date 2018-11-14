package part2Actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorCounterExercise extends App{

  object CounterActor{
    case object Increment
    case object Decrement
    case object Print
  }

  class CounterActor extends Actor{
    import CounterActor._

    private var counter: Int = 0

    override def receive: Receive = {
      case Increment => this.counter += 1
      case Decrement => this.counter -= 1
      case Print => println(s"Current counter: ${this.counter}")
    }
  }

  val actorSystem = ActorSystem("ActorCounterExercise")
  val counterActor = actorSystem.actorOf(Props[CounterActor],"counterActor")

  counterActor ! CounterActor.Increment
  counterActor ! CounterActor.Increment
  counterActor ! CounterActor.Increment
  counterActor ! CounterActor.Increment
  counterActor ! CounterActor.Print
  counterActor ! CounterActor.Decrement
  counterActor ! CounterActor.Decrement
  counterActor ! CounterActor.Decrement
  counterActor ! CounterActor.Print

}
