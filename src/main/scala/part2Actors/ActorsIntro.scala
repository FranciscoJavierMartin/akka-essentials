package part2Actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorsIntro extends App{

  // Part1 - actor systems
  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  // Part2 - create actors
  // word count actor

  class WordCountActor extends Actor {
    // internal data
    var totalWords = 0

    // behaviour
    def receive: PartialFunction[Any,Unit] ={
      case message: String =>
        println(s"[word counter] I have received: $message")
        totalWords += message.split(" ").length
      case msg => println(s"[word counter] I Cannot understand ${msg.toString}")
    }
  }

  // Part3 - instantiate our actor
  val wordCounter = actorSystem.actorOf(Props[WordCountActor],"wordCounter")
  val anotherWordCounter = actorSystem.actorOf(Props[WordCountActor],"anotherWordCounter")

  // Part4 - communicate!
  wordCounter ! "I am learning Akka an it's pretty damn cool!" // "tell"
  anotherWordCounter ! "A different message"
  // asynchronous

  object Person{
    def props(name:String) = Props(new Person(name))
  }

  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi, my name is $name")
      case _ =>
    }
  }

  val person = actorSystem.actorOf(Person.props("Bob"))
  person ! "hi"
}
