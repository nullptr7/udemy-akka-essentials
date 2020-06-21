package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps

object TimersAndSchedulers extends App {

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("SchedulersTimesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")
  implicit val context: ExecutionContextExecutor = system.dispatcher

  system.log.info("Scheduling reminder for simpleActor")
  system.scheduler.scheduleOnce(1 second) {
    simpleActor ! "reminder"
  }

  val runnableThread: Runnable = () => {
    simpleActor ! "heartbeat"
  }
  /*
    val routine = system.scheduler.schedule(1 second, 2 second) {
      simpleActor ! "heartbeat"
    }
  */

  // This is a demo for repeatedScheduler
  val routineNonDeprecated: Cancellable =
    system.scheduler.scheduleAtFixedRate(1 second, 2 seconds)(runnableThread)

  system.scheduler.scheduleOnce(5 seconds) {
    routineNonDeprecated.cancel()
  }

  /**
   * Exercise
   * Implement a self closing actor
   *  - if the actor receives a message, can be anything, you have 1 second to send it another message
   *  - if the time window expires, the actor will stop itself, if you send another message the time window is reset
   *    and you have one more second to send the message
   */


}
