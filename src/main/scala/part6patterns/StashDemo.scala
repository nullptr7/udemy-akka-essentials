package part6patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashDemo extends App {

  /*
      Lets assume we have resource actor
        - open => it can read/write requests to the resource
        - otherwise it will postpone all read/write request until the state is opened

      Initially resourceActor is closed,
        open -> when it receive message it goes in open state
        read -> read write messages are postponed

      when resourceActor is open it can
        - read and write message
        - receive close message "switch to close state"

      [Open, Read, Read, Write]
        - Switch to open state
        - read the data
        - read the data again
        - write the data


      [Read, Open, Write]
        - Put Read in stash cz the actor is not open
        - open -> switch state to open
        -   mailbox => [Read, Write]
            now these two operations are handled


   */

  case object Open

  case object Close

  case object Read

  case class Write(data: String)


  // Step 1 - Mixin the stash trait
  class ResourceActor extends Actor with ActorLogging with Stash {
    private var innerData: String = ""

    override def receive: Receive = closed

    def closed: Receive = {
      case Open =>
        log.info("opening resource...")
        unstashAll() // Step 3 Before we switch the context we will unstashAll() things
        context.become(open)
      case message =>
        log.info(s"stashing $message because I have not opened yet!")
        stash() // Step 2- things we cannot handle we stash it
    }

    def open: Receive = {

      case Read =>
        // do some actual computation
        log.info(s"I have read $innerData")
      case Write(data) =>
        log.info(s"I am writing $data")
        innerData = data
      case Close =>
        log.info("closing resource...")
        unstashAll()
        context.become(closed)
      case message =>
        log.info(s"I cannot handle message ${message} it in open state")
        stash()

    }
  }

  val system = ActorSystem("StashDemo")

  val resourceActor = system.actorOf(Props[ResourceActor], "resourceActor")

  //  resourceActor ! Write("I love stash")
  //  resourceActor ! Read
  //  resourceActor ! Open


  // Exercise -- what would be the output below
  resourceActor ! Read // will be stashed
  resourceActor ! Open // switch to open -- and pop read from stash
  resourceActor ! Open // will be stashed
  resourceActor ! Write("I love stash") // will be handled, hence output will be I am writing "I love stash"
  resourceActor ! Close // switch to close; will switch to open again since open was stashed before
  resourceActor ! Read // will print "I love stash"

}
