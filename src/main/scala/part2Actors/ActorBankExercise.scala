package part2Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2Actors.ActorBankExercise.Client.LiveTheLife

object ActorBankExercise extends App{

  object BankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object Statement

    case class TransactionSuccess(message:String)
    case class TransactionFailure(message:String)
  }

  class BankAccount extends Actor{
    import BankAccount._

    private var funds = 0

    override def receive: Receive = {
      case Deposit(amount) =>
        if(amount<0) sender() ! TransactionFailure("Invalid deposit amount")
        else{
          funds+=amount
          sender() ! TransactionSuccess(s"Successfully deposited $amount")
        }
      case Withdraw(amount) =>
        if(amount<0) sender() ! TransactionFailure("Invalid withdraw amount")
        else if(amount>funds) sender() ! TransactionFailure("Insufficient funds")
        else{
          funds -= amount
          sender() ! TransactionSuccess(s"Successfully withdraw $amount")
        }
      case Statement => sender() ! s"Your balance is $funds"
    }
  }

  object Client {
    case class LiveTheLife(account: ActorRef)
  }

  class Client extends Actor{
    import Client._
    import BankAccount._

    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! Deposit(10000)
        account ! Withdraw(90000)
        account ! Withdraw(500)
        account ! Statement
      case message => println(message.toString)
    }
  }

  val system = ActorSystem("actorBankExercise")
  val account = system.actorOf(Props[BankAccount],"bankAccount")
  val client = system.actorOf(Props[Client],"billionaire")

  client ! LiveTheLife(account)
}
