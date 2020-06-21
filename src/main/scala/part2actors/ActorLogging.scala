package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

object ActorLogging extends App {

  class SimpleActorWithExplicitLogging extends Actor {

    // Method #1
    private val logger = Logging(context.system, this)

    override def receive: Receive = {
      case message => logger.info(message.toString)
    }
  }

  val system = ActorSystem("LoggingDemo")

  val actorLogging = system.actorOf(Props[SimpleActorWithExplicitLogging])

  actorLogging ! "Simple Logging message"

  // Method #2 ActorLogging
  class ActorWithLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case (a, b) => log.info("Two things: {} and {}", a, b)
      case message: String => log.info(message)
    }
  }

  val actorWithLogging = system.actorOf(Props[ActorWithLogging])
  actorWithLogging ! "This is another message from actor with logging"
  actorWithLogging ! (42, 55)
}
