package exercises.part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import exercises.part2actors.ActorPlayground.Bank.{Failure, Statement, Success}

object ActorPlayground extends App {

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

  val system = ActorSystem("ActorPlayground")


  object CounterActor {

    case object Increment

    case object Decrement

    case object Print

  }

  class CounterActor extends Actor {
    var counter = 0

    import CounterActor._

    override def receive: Receive = {
      case Increment => counter += 1
      case Decrement => counter -= 1
      case Print => println(s"[print counter] final count is $counter")
    }
  }


  val counterActor = system.actorOf(Props[CounterActor], "counterActor")

  import CounterActor._

  for (_ <- 1 to 3) counterActor ! Increment
  for (_ <- 1 to 10) counterActor ! Decrement

  //counterActor ! Print

  class Bank extends Actor {
    var balance = 10000

    override def receive: Receive = {
      case Transaction(amt, "Deposit") =>
        balance += amt
        self ! Success("Deposit succeeded")

      case Transaction(amt, "Withdrawal") =>
        if (balance > amt) {
          balance -= amt
          self ! Success("Withdrawal succeeded")
        } else {
          self ! Failure("Low amount")
        }
      case Success(msg: String) => println(s"[bank] $msg")
      case Failure(reason: String) => println(s"[bank] Transaction failed due to $reason")
      case Statement => sender() ! s"[bank] Your account balance is $balance"
    }
  }

  object Bank {

    case class Success(message: String)

    case class Failure(reason: String)

    case object Statement

  }

  class Person(ref: ActorRef) extends Actor {
    override def receive: Receive = {
      case 1 => ref ! Statement
      case message: String => println(s"$message")
    }
  }

  case class Transaction(amt: Int, transType: String)

  val bankActor = system.actorOf(Props[Bank], "BankActor")
  val person = system.actorOf(Props(new Person(bankActor)), "Person")
  bankActor ! Transaction(100, "Deposit")
  bankActor ! Transaction(100, "Deposit")
  bankActor ! Transaction(100, "Deposit")
  bankActor ! Transaction(100, "Withdrawal")
  bankActor ! Transaction(100000, "Withdrawal")
  bankActor ! Transaction(10000, "Deposit")
  Thread.sleep(1000)
  person ! 1
}
