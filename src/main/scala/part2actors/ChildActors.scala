package part2actors

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, Props}
import part2actors.ChildActors.CreditCard.CheckStatus
import part2actors.ChildActors.Parent.{CreateChild, TellChild}

object ChildActors extends App {

  object Parent {

    case class CreateChild(name: String)

    case class TellChild(message: String)

  }

  class Parent extends Actor {

    import Parent._

    //    var child: ActorRef = _

    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"${self.path} creating child")
        context.become(saveChild(context.actorOf(Props[Child], name)))
      //        val childRef = context.actorOf(Props[Child], name)
      //        child = childRef
    }

    def saveChild(childRef: ActorRef): Receive = {
      case TellChild(message) =>
        if (childRef != null) childRef forward message
    }

  }

  class Child extends Actor {
    override def receive: Receive = {
      case message => println(s"${self.path} I got: $message")
    }
  }

  val sys = ActorSystem("ChildActors")
  val parent = sys.actorOf(Props[Parent], "SingleParent")
  parent ! CreateChild("NullPointer")
  parent ! TellChild("RaiseAnException")


  /**
   * Actor selection
   */
  val childSelection: ActorSelection = sys.actorSelection("/user/SingleParent/NullPointer")
  childSelection ! "RaisingExceptionBadly"

  /*
      NEVER PASS MUTABLE ACTORS STATE, OR THE 'THIS' REFERENCE TO CHILD ACTORS
      THIS WILL BREAK THE CORE BASIS OF ACTOR PRINCIPLE
      E.G. AS BELOW ISSUE
   */

  object NaiveBankAccount {

    case class Deposit(amt: Int)

    case class Withdraw(amt: Int)

    case object InitializeAccount

  }

  class NaiveBankAccount extends Actor {

    import CreditCard._
    import NaiveBankAccount._

    var amount = 0

    override def receive: Receive = {
      case InitializeAccount =>
        val creditCardRef = context.actorOf(Props[CreditCard], "card")
        creditCardRef ! AttachToAccount(this) ///This is really bad!!!

      case Deposit(amt) => deposit(amt)
      case Withdraw(amt) => withdraw(amt)

    }

    def deposit(amt: Int): Unit = {
      println(s"[bank] - depositing amount $amt to $amount")
      amount += amt
    }

    def withdraw(amt: Int): Unit = {
      println(s"[bank] - withdrawing amount $amt from $amount")
      amount -= amt
    }
  }

  object CreditCard {

    case class AttachToAccount(bankAccount: NaiveBankAccount)

    case object CheckStatus

  }

  class CreditCard extends Actor {

    import CreditCard._

    override def receive: Receive = {
      case AttachToAccount(account) => context.become(attachToAccount(account))
    }

    def attachToAccount(account: NaiveBankAccount): Receive = {
      case CheckStatus =>
        println(s"${self.path} you message has been processed")
        account.withdraw(100)
    }
  }

  import NaiveBankAccount._

  val bankAccountRef = sys.actorOf(Props[NaiveBankAccount], "account")
  bankAccountRef ! InitializeAccount

  bankAccountRef ! Deposit(1000)
  Thread.sleep(500)

  val creditCardRef = sys.actorSelection("/user/account/card")
  creditCardRef ! CheckStatus
}
