package part6patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

// Step 1 - import the ask pattern
import akka.pattern.ask
import akka.pattern.pipe

class AskSpec extends TestKit(ActorSystem("AskSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll{

  override def afterAll(): Unit ={
    TestKit.shutdownActorSystem(system)
  }

  import AskSpec._

  "An authenticator" should {
    authenticatorTestSuite(Props[AuthManager])
  }

  "An piped authenticator" should {
    authenticatorTestSuite(Props[PipeAuthManager])
  }

  def authenticatorTestSuite(props:Props) = {
      import AuthManager._


    "fail to authenticate a non-registered user" in {
      val authManager = system.actorOf(props)
      authManager ! Authenticate("daniel", "rtjvm")
      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }

    "fail to authenticate if invalid password" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("daniel", "rtjvm")
      authManager ! Authenticate("daniel", "iloveakka")
      expectMsg(AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT))
    }

    "successfully authenticate a registered user" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("daniel", "rtjvm")
      authManager ! Authenticate("daniel", "rtjvm")
      expectMsg(AuthSuccess)
    }

  }
}

object AskSpec {

  // this code is somewhere else in your app
  case class Read(key:String)
  case class Write(key:String,value:String)
  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())

    def online(kv: Map[String,String]): Receive = {
      case Read(key) =>
        log.info(s"Trying to read the value at the key $key")
        sender() ! kv.get(key) // Option[String]
      case Write(key,value) =>
        log.info(s"Writting the value $value for the key $key")
        context.become(online(kv+(key->value)))
    }
  }

  // User athencicator actor
  case class RegisterUser(username:String, password: String)
  case class Authenticate(username:String, password: String)
  case class AuthFailure(message:String)
  case object AuthSuccess

  object AuthManager{
    val AUTH_FAILURE_NOT_FOUND = "username not found"
    val AUTH_FAILURE_PASSWORD_INCORRECT = "password incorrect"
    val AUTH_FAILURE_SYSTEM = "system error"
  }

  class AuthManager extends Actor with ActorLogging {
    import AuthManager._

    // Step 2 - logistics
    implicit val timeout: Timeout = Timeout(1 second)
    implicit val executionContext: ExecutionContext = context.dispatcher

    protected val authDB = context.actorOf(Props[KVActor])

    override def receive: Receive = {
      case RegisterUser(username,password) => authDB ! Write(username, password)
      case Authenticate(username,password) => handleAuthetication(username,password)

    }

    def handleAuthetication(username:String, password: String):Unit ={
      val originalSender = sender()
      // Step 3 -ask the actor
      val future = authDB ? Read(username)
      // Step 4 - handle the future for e.g with onComplete
      future.onComplete {
        // Step 5 most important
        // Never call methods on the actor instance or access mutable state in onComplete
        // avoid closing over the actor instance or mutable state
        case Success(None) => originalSender ! AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Success(Some(dbPassword)) =>
          if(dbPassword == password)
            originalSender ! AuthSuccess
          else
            originalSender ! AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
        case Failure(_) => originalSender ! AuthFailure(AUTH_FAILURE_SYSTEM)
      }
    }
  }

  class PipeAuthManager extends AuthManager {
    import AuthManager._

    override def handleAuthetication(username: String, password: String): Unit = {
      // Step 3 - ask the actor
      val future: Future[Any] = authDB ? Read(username)
      // Step 4 - process the future until to get the responses you will send back
      val passwordFuture: Future[Option[String]] = future.mapTo[Option[String]]
      val responseFuture = passwordFuture.map{
        case None => AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Some(dbPassword) =>
          if(dbPassword == password)
            AuthSuccess
          else
            AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
      }

      // Step 5 - pipe the resulting future to the actor you want to send the result to
      /*
        When the future completes, send the response to the actor ref in the arg list
       */
      responseFuture.pipeTo(sender())
    }
  }
}
