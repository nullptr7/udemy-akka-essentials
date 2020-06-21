package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App {

  class SimpleActor extends Actor {

    override def receive: Receive = {
      case "Hi" => context.sender() ! "Hello there!" //replying to message

      case message: String =>
        //        println(sender().path)
        println(s"[${context.self}] I have received a String Message $message")

      case number: Int => println(s"[simple actor] I have received a Int Number $number")

      case SpecialMessage(content) => println(s"[simple actor] I have received a SpecialMessage $content")

      case SendMessageToYourself(content) => self ! content

      case SayHiTo(ref) => ref ! "Hi"

      case WirelessPhoneMessage(content, ref) =>
        ref forward (content + "s" + ref.path) //I Keep the original sender of the WirelessPhoneMessage
    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")

  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  simpleActor ! "Hello Actor"

  // 1. Messages can be of anytype
  // a. Messages must be Immutable
  // b. Messages must be Serializable
  simpleActor ! 42

  case class SpecialMessage(content: String)

  simpleActor ! SpecialMessage("A special message from custom class")

  // 2 - actors have information about their context and about themselves
  // context.self === 'this'
  case class SendMessageToYourself(content: String)

  simpleActor ! SendMessageToYourself("I am an actor and I am proud of it!")

  // 3 - Actors can reply to messages
  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")
  val phil = system.actorOf(Props[SimpleActor], "phil")

  case class SayHiTo(ref: ActorRef)

  alice ! SayHiTo(bob)
  alice ! SayHiTo(phil)

  // 4- dead letters
  alice ! "Hi"

  // 5 - forwarding messages
  // D -> A -> B
  // Forwarding = sending a message with Original Sender

  case class WirelessPhoneMessage(content: String, ref: ActorRef)

  alice ! WirelessPhoneMessage("Hi", bob)

  /**
   * Exercises
   *  1. Create a counter actor, hold an internal variable which will respond to
   *    - Increment
   *    - Decrement
   *    - Print
   *
   *  2. A bankAccount as an actor
   *
   * / Receives
   *      - receive message to deposit
   *      - withdraw an amount
   *      - statement
   *
   * / Replies
   *      - Reply with success or failure
   *
   * interact with some other kind of actor
   */

}
