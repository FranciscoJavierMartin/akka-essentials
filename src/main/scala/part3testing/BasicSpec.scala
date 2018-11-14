package part3testing

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll{

  // Setup
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "The thing being tested" should {
    "do this" in {
      // testing scenario
    }
  }
}
