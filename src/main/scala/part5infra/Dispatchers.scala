package part5infra

import akka.actor.Props

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object Dispatchers extends App {

  class Counter extends akka.actor.Actor with akka.actor.ActorLogging {
    var count = 0

    override def receive: Receive = {
      case message =>
        count += 1
        log.info(s"[$count] $message")
    }
  }

  val sys = akka.actor.ActorSystem("DispatchersDemo")

  // Method 1 : Using programmatic/in code
  //  private val actors: IndexedSeq[ActorRef] =
  //    for (index <- 1 to 10) yield sys.actorOf(Props[Counter].withDispatcher("my-dispatcher"), s"counter_$index")
  val random = new Random()
  //  for (i <- 1 to 1000) {
  //    actors(random.nextInt(10)) ! i
  //  }

  // Method 2: from config
  private val rtjvmActor = sys.actorOf(Props[Counter], s"rtjvm")

  //  for (i <- 1 to 1000) rtjvmActor ! i

  /**
   * Dispatchers implement the ExecutionContext trait
   */

  class DBActor extends akka.actor.Actor with akka.actor.ActorLogging {

    implicit val executionContext: ExecutionContext = context.dispatcher

    override def receive: Receive = {
      // wait on a resource
      case message => Future {
        Thread.sleep(5000)
        log.info(s"success: $message")
      }
    }
  }

  val dbActor = sys.actorOf(Props[DBActor], "dbActor")
//  dbActor ! "the meaning of life is 42"

  val nonBlockingActor = sys.actorOf(Props[Counter])

  for(i <- 1 to 1000) {
    val message = s"important message $i"
    nonBlockingActor ! message
    dbActor ! message
  }

}
