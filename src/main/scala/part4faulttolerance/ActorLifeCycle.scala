package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

object ActorLifeCycle extends App {


  case object StartChild

  class LifeCycleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case StartChild =>
        context.actorOf(Props[LifeCycleActor], "child")

    }

    override def preStart(): Unit = log.info("I am starting")

    override def postStop(): Unit = log.info("I have stopped")
  }

  val system = ActorSystem("LifeCycleDemo")
  val parent: ActorRef = system.actorOf(Props[LifeCycleActor], "Parent")

  //  parent ! StartChild
  //  parent ! PoisonPill

  case object Check

  case object Fail

  case object CheckChild

  case object FailChild

  class Child extends Actor with ActorLogging {

    override def preStart(): Unit = log.info("supervised child started")

    override def postStop(): Unit = log.info("supervised child stopped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.info(s"supervised actor restarting because of ${reason.getMessage}")
    }

    override def postRestart(reason: Throwable): Unit = {
      log.info("supervised actor restarted!")
    }

    override def receive: Receive = {
      case Fail =>
        log.warning("child will fail now")
        throw new RuntimeException("I failed!!")

      case Check =>
        log.info("alive and kicking..!")
    }
  }

  class Parent extends Actor with ActorLogging {
    private val child: ActorRef = context.actorOf(Props[Child], "supervisedChild")

    override def receive: Receive = {
      case FailChild => child ! Fail
      case CheckChild => child ! Check
    }
  }


  val supervisor: ActorRef = system.actorOf(Props[Parent], "supervisor")
  supervisor ! FailChild

}
