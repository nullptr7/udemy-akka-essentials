package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChangingActorBehaviour.Mom.MomStart

object ChangingActorBehaviour extends App {

  object FuzzyKid {

    case object KidAccept

    case object KidReject

    val HAPPY = "happy"
    val SAD = "sad"
  }

  class FuzzyKid extends Actor {

    import FuzzyKid._

    var state: String = HAPPY

    import Mom._

    override def receive: Receive = {
      case Food(VEGETABLE) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) => if (state == HAPPY) sender() ! KidAccept else sender() ! KidReject
    }
  }

  class StatelessFuzzyKid extends Actor {

    import FuzzyKid._
    import Mom._

    override def receive: Receive = {
      happy
    }

    def happy: Receive = {
      case Food(VEGETABLE) => context.become(sad, discardOld = false) //change my receive handler to sad receive
      case Food(CHOCOLATE) =>
      case Ask(_) => sender() ! KidAccept
    }

    def sad: Receive = {
      case Food(VEGETABLE) => context.become(sad, discardOld = false) // should be sad and we are already in sad
      case Food(CHOCOLATE) => context.unbecome()
      case Ask(_) => sender() ! KidReject
    }
  }

  object Mom {

    case class MomStart(ref: ActorRef)

    case class Food(food: String)

    case class Ask(msg: String) // will be a question mom will ask like do you want to play

    val VEGETABLE = "veggies"
    val CHOCOLATE = "chocolate"

  }

  class Mom extends Actor {

    import FuzzyKid._
    import Mom._

    override def receive: PartialFunction[Any, Unit] = {
      case MomStart(kidRef) =>
        // Test our interaction
        kidRef ! Food(VEGETABLE)
        kidRef ! Food(VEGETABLE)
        kidRef ! Food(VEGETABLE)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Ask("do you want to play?")
      case KidAccept => println("Yay! my kid is happy")
      case KidReject => println("My kid is sad, but he has eaten healthy food")
    }
  }

  val system = ActorSystem("changeActorBehaviour")
  val fuzzyKid = system.actorOf(Props[FuzzyKid], "theFuzzyKid")
  val mom = system.actorOf(Props[Mom], "theMom")

  //mom ! MomStart(fuzzyKid)

  /*
      Mom receives momStart
      Kid receives fruit(VEGETABLE) Kid will change the handler to sad receive, so all the future messages sadReceive will be used
      kid receives ask(do you want to play) Kid replies with sadReceive handler

      -- Kid will change the handler to sad receive, so all the future messages sadReceive will be used
      -- Until it changes to Food(Chocolate)
   */
  val statelessFuzzyKid = system.actorOf(Props[StatelessFuzzyKid], "newKid")
  mom ! MomStart(statelessFuzzyKid)

  /*
      New behaviour
      Food(veg) sadReceive.push
      Food(veg) sadReceive.push
      Food(choco) sadReceive.pop
      Food(choco) sadReceive.pop

      stack:
      1. happyReceive
   */

  /**
   * Exercise
   * 1 - Recreate a counter actor with context.become and no mutable state
   * 2 - Simplified voting system
   *      - We have two kind of actors (citizen,
   */

  case class Vote(candidate: String)
  // Will handle messages for voting
  class Citizen extends Actor {
    override def receive: Receive = ???
  }

  case class AggregateVotes(citizen: Set[ActorRef])
  case object VoteStatusRequest
  // Send messages to citizen who they have voted for
  class VoteAggregator extends Actor {
    override def receive: Receive = ???
  }


}
