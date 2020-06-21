package part4faulttolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.language.postfixOps

class SupervisionSpec extends TestKit(ActorSystem("SupervisionSpec")) with ImplicitSender with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import SupervisionSpec._

  "A Supervisor" should {
    "resume its child in-case of minor fault" in {
      val supervisor: ActorRef = system.actorOf(Props[Supervisor])
      supervisor ! Props[FuzzyWordCounter]
      val child: ActorRef = expectMsgType[ActorRef]

      child ! "I Love Akka"
      child ! Report
      expectMsg[Int](3)


      child ! "I Love Akka its awesome and it is different from spring reactive even though similar :)"
      child ! Report
      expectMsg[Int](3) //Because it resumes from where it left successfully. Hence the state is not changed.

      child ! ""
      child ! Report
      //This has to change, because the child will be restarted from the very same input.
      // Restart destroys the state of the child
      expectMsg[Int](0)

    }

    "terminate if the first letter is not uppercase" in {
      val supervisor: ActorRef = system.actorOf(Props[Supervisor], "SupervisorTerminated")
      supervisor ! Props[FuzzyWordCounter]
      val child: ActorRef = expectMsgType[ActorRef]
      watch(child)

      child ! "i love akka"

      val terminated = expectMsgType[Terminated]
      assert(terminated.actor == child)
    }

    "escalate an error when it doesn't know what to do" in {
      val supervisor: ActorRef = system.actorOf(Props[Supervisor], "SupervisorEscalated")
      supervisor ! Props[FuzzyWordCounter]
      val child: ActorRef = expectMsgType[ActorRef]

      watch(child)
      child ! 43

      val terminated = expectMsgType[Terminated]
      assert(terminated.actor == child)
      child ! Report

    }
  }

  "A kinder supervisor" should {
    "not kill children in case it's restarted or escalated failures" in {

      val supervisor: ActorRef = system.actorOf(Props[NoDeathOnRestartSupervisor], "KinderSupervisor")
      supervisor ! Props[FuzzyWordCounter]
      val child: ActorRef = expectMsgType[ActorRef]

      child ! "Akka is cool"
      child ! Report
      expectMsg[Int](3)

      child ! 45
      child ! Report
      expectMsg[Int](0)
    }
  }

  "An allForOneSupervisor" should {
    "apply allForOne strategy" in {
      val supervisor = system.actorOf(Props[AllForOneSupervisor], "allForOneSupervisor")
      supervisor ! Props[FuzzyWordCounter]
      val child = expectMsgType[ActorRef]

      supervisor ! Props[FuzzyWordCounter]
      val secondChild = expectMsgType[ActorRef]
      secondChild ! "Akka Message"
      secondChild ! Report
      expectMsg(2)

      // asserts child throws NPE
      EventFilter[NullPointerException]() intercept {
        child ! ""
      }

      Thread.sleep(500)
      secondChild ! Report
      expectMsg(0)
    }
  }

}

object SupervisionSpec {

  class Supervisor extends Actor {

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }

    override def receive: Receive = {
      case props: Props =>
        val childRef = context.actorOf(props)
        sender() ! childRef
    }
  }

  class NoDeathOnRestartSupervisor extends Supervisor {
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      // empty because we do not want to kill i.e. restart
    }
  }

  class AllForOneSupervisor extends Supervisor {
    override def supervisorStrategy: AllForOneStrategy = AllForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }
  }

  case object Report

  class FuzzyWordCounter extends Actor {
    var words: Int = 0

    override def receive: Receive = {
      case "" => throw new NullPointerException("Sentence is empty")
      case sentence: String =>
        if (sentence.length > 20) throw new RuntimeException("Sentence is too big")
        else if (!Character.isUpperCase(sentence(0))) throw new IllegalArgumentException("Sentence must start with uppercase")
        else words += sentence.split(" ").length
      case Report => sender() ! words
      case _ => throw new Exception("Can only receive strings")
    }
  }

}
