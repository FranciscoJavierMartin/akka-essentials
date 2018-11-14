package part2Actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorCounterAdvanced extends App{
  /**
    Exercise
    1 - Recreate the Counter Actor with context.become and NO MUTABLE STATE
    */

  object Counter{
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter extends Actor{
    import Counter._

    override def receive: Receive = countReceive(0)

    def countReceive(currentCount: Int): Receive = {
      case Increment =>
        println(s"[countReceive($currentCount)] incrementing")
        context.become(countReceive(currentCount+1))
      case Decrement =>
        println(s"[countReceive($currentCount)] decrementing")
        context.become(countReceive(currentCount-1))
      case Print => println(s"[Counter] my current count is $currentCount")
    }
  }

  import Counter._
  val system = ActorSystem("ActorCounterAdvanced")
  val counter = system.actorOf(Props[Counter],"myCounter")

  (1 to 5).foreach(_=> counter ! Increment)
  (1 to 3).foreach(_=> counter ! Decrement)
  counter ! Print
}
