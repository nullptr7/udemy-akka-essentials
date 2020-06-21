package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration._
import scala.language.postfixOps


/**
 * Exercise
 * Implement a self closing actor
 *  - if the actor receives a message, can be anything, you have 1 second to send it another message
 *  - if the time window expires, the actor will stop itself, if you send another message the time window is reset
 * and you have one more second to send the message
 */
object SelfClosingActorExample extends App {

  type SCT = PartialFunction[Any, Unit]

  class SelfClosingActor extends Actor with ActorLogging {

    private var schedules: Cancellable = createTimeWindow

    def createTimeWindow: Cancellable = {
      context.system.scheduler.scheduleOnce(100 second) {
        self ! "timeout"
      }(system.dispatcher)
    }

    override def receive: SCT = {
      case "timeout" =>
        log.info("stopping myself")
        context.stop(self)
      case message =>
        log.info(s"received message '$message'")
        schedules.cancel()
        schedules = createTimeWindow

        /*
            Below line has to be uncommented and 'schedules = createTimeWindow' has to be commented
            context.become(switchTimeout(createTimeWindow))
         */
    }

    /**
     * created my own method that handles mutability of schedules variable
     *
     * code has to be added a part of case to handle the timeout scenario. However I am guessing
     * even the 'case message => ...' also has to be added not sure need to look at.
     *
     * case "timeout" =>
     *         log.info("stopping myself")
     *         context.stop(self)
     *
     * @param schedules
     * @return
     */
    def switchTimeout(schedules: Cancellable): SCT = {
      case _ =>
        if (schedules.isCancelled) {
          log.info("I am here")
          context.become(switchTimeout(createTimeWindow))
        }
        else {
          log.info("I am here2")
          context.become(switchTimeout(schedules))
        }
    }
  }

  val system = ActorSystem("SchedulersTimesDemo")
  val selfClosingActor = system.actorOf(Props[SelfClosingActor], "selfClosingActor")

  /*  system.scheduler.scheduleOnce(250 millis) {
      selfClosingActor ! "ping"
      selfClosingActor ! "ping"
      selfClosingActor ! "ping"
      selfClosingActor ! "ping"
    }(system.dispatcher)
    system.scheduler.scheduleOnce(1 seconds) {
      system.log.info("sending pong to self closing actor")
      selfClosingActor ! "pong"
    }(system.dispatcher)*/

  /**
   * We can send messages to itself using Timer, this is bit more safer.
   *
   * Below is an example of usage of timers to handle mes
   */

  case object TimerKey

  case object Start

  case object Reminder

  case object Stop

  class TimerBasedHeartbeatActor extends Actor with ActorLogging with Timers {

    timers.startSingleTimer(TimerKey, Start, 500 millis)

    override def receive: SCT = {
      case Start =>
        log.info("bootstrapping")
        timers.startTimerAtFixedRate(TimerKey, Reminder, 1 second)

      case Reminder => log.info("I am alive")

      case Stop =>
        log.warning("stopping!")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }

  val timerBasedHeartbeatActor = system.actorOf(Props[TimerBasedHeartbeatActor], "HeartbeatActor")
  system.scheduler.scheduleOnce(5 seconds) {
    timerBasedHeartbeatActor ! Stop
  }(system.dispatcher)

}



