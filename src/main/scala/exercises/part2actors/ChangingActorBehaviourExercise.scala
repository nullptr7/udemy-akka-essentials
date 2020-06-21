package exercises.part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChangingActorBehaviourExercise extends App {

  /**
   * Exercise
   * 1 - Recreate a counter actor with context.become and no mutable state
   *
   */

  val system = ActorSystem("ChangingActorBehaviourExercise")


  object CounterActor {

    case object Increment

    case object Decrement

    case object Print

  }

  class CounterActor extends Actor {

    import CounterActor._

    def check(counter: Int): Receive = {
      case Increment =>
        println(s"[counterActor] Incrementing counter[$counter]... to ($counter) + " + 1)
        context.become(check(counter + 1))
      case Decrement =>
        println(s"[counterActor] Decrementing 1... from $counter")
        context.become(check(counter - 1))
      case Print => println(s"[counterActor] final count is $counter")
    }

    override def receive: Receive = check(0)
  }


  val counterActor = system.actorOf(Props[CounterActor], "counterActor")

  import CounterActor._

  for (_ <- 1 to 1) counterActor ! Increment
  for (_ <- 1 to 3) counterActor ! Decrement
  counterActor ! Print

  /**
   * 2 - Simplified voting system
   *      - We have two kind of actors (citizen,
   */
  case class Vote(candidate: String)

  case object VoteStatusRequest

  case class VoteStatusReply(candidate: Option[String])

  // Will handle messages for voting
  class Citizen extends Actor {
    var candidate: Option[String] = None

    override def receive: Receive = {
      case Vote(c) => candidate = Some(c)
      case VoteStatusRequest => sender() ! VoteStatusReply(candidate)
    }
  }

  class CitizenWithoutMutable extends Actor {
    override def receive: Receive = {
      case Vote(c) => context.become(voted(c))
      case VoteStatusRequest => sender() ! VoteStatusReply(None)
    }

    /*
        This will be executed only when voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel)) is called
     */
    def voted(c: String): Receive = {
      case VoteStatusRequest => sender() ! VoteStatusReply(Some(c))
    }
  }

  case class AggregateVotes(citizen: Set[ActorRef])


  // Send messages to citizen who they have voted for
  class VoteAggregator extends Actor {

    var stillWaiting: Set[ActorRef] = Set()
    var currentStatus: Map[String, Int] = Map()

    override def receive: Receive = {
      case AggregateVotes(citizens) =>
        stillWaiting = citizens
        citizens.foreach(_ ! VoteStatusRequest)

      case VoteStatusReply(None) =>
        // A candidate hasn't voted
        sender() ! VoteStatusRequest // of-course this might end up in infinite loop

      case VoteStatusReply(Some(candidate)) =>
        val newStillWaiting = stillWaiting - sender()
        val currentVotesOfCandidate = currentStatus.getOrElse(candidate, 0)
        currentStatus = currentStatus + (candidate -> (currentVotesOfCandidate + 1))
        if (newStillWaiting.isEmpty)
          println(s"[aggregator] - total number of votes $currentStatus")
        else {
          println(s"[aggregator] - still waiting for ${newStillWaiting.size} citizens to vote")
          stillWaiting = newStillWaiting
        }
    }
  }

  class VotesAggregatorWithoutMutable extends Actor {
    override def receive: Receive = awaitingCommand

    def awaitingCommand: Receive = {
      case AggregateVotes(citizens) =>
        citizens.foreach(_ ! VoteStatusRequest)
        context.become(awaitingStatus(citizens, Map()))
    }

    def awaitingStatus(stillWaiting: Set[ActorRef], currentStatus: Map[String, Int]): Receive = {
      case VoteStatusReply(None) =>
        sender() ! VoteStatusRequest
      case VoteStatusReply(Some(candidate)) =>
        val newStillWaiting = stillWaiting - sender()
        val currentVotesOfCandidate = currentStatus.getOrElse(candidate, 0)
        val newStats = currentStatus + (candidate -> (currentVotesOfCandidate + 1))
        if (newStillWaiting.isEmpty)
          println(s"[aggregator] - total number of votes $newStats")
        else {
          println(s"[aggregator] - still waiting for ${newStillWaiting.size} citizens to vote")
          context.become(awaitingStatus(newStillWaiting, newStats))
        }
    }
  }

  val alice = system.actorOf(Props[CitizenWithoutMutable] )
  val bob = system.actorOf(Props[CitizenWithoutMutable])
  val charlie = system.actorOf(Props[CitizenWithoutMutable])
  val daniel = system.actorOf(Props[CitizenWithoutMutable])

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  daniel ! Vote("Roland")
  charlie ! Vote("Poland")

  val voteAggregator = system.actorOf(Props[VotesAggregatorWithoutMutable])
  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))
  /*
    print the status of the votes

    Martin - 1
    Jonas - 1
    Roland - 2
   */

}
