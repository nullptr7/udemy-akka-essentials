package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

object Mailboxes extends App {

  val system = ActorSystem("mailboxDemo", ConfigFactory.load().getConfig("mailboxesDemo"))

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message =>
        log.info(message.toString)
    }
  }

  /**
   * Interesting case
   * #1 A custom priority mailbox
   *
   * if ticket name starts with P0 - high priority
   * P1
   * P2
   * P3
   * P4
   */

  // Step 1 - mailbox definition
  class SupportTicketPriorityMailbox(settings: ActorSystem.Settings, config: Config)
    extends UnboundedPriorityMailbox(PriorityGenerator {
      case message: String if message.startsWith("[P0]") => 0
      case message: String if message.startsWith("[P1]") => 1
      case message: String if message.startsWith("[P2]") => 2
      case message: String if message.startsWith("[P3]") => 3
      case _ => 4
    })

  // step 2 - make it known in config(refer application.conf)

  // step 3 - attach the dispatcher to an actor
  val supportTicketLogger = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"), "supportTicketLogger")

  //  supportTicketLogger ! PoisonPill
  //  Thread.sleep(1000)
    supportTicketLogger ! "[P3] this thing would be nice to have!"
    supportTicketLogger ! "[P2] this thing needs to be solved now"
    supportTicketLogger ! "[P1] this thing can be solved when you have time"

  // after which time can I send another message and be prioritized accordingly? -- you cannot identify


  /**
   * Interesting case # 2 - Control aware mailbox (some message needs to be processed first regardless)
   *
   * we'll use unbounded mailbox
   */

  // Step 1 - mark the important messages as control messages
  case object ManagementTicket extends ControlMessage

  // Step 2 - Configure who gets the mailbox
  // configure the actor itself to have the mailbox instead of dispatcher

  // Method 1
  val controlAwareActor = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"))

//  controlAwareActor ! "[P1] this thing can be solved when you have time"
//  controlAwareActor ! "[P2] this thing needs to be solved now"
//  controlAwareActor ! "[P3] this thing would be nice to have!"
//  controlAwareActor ! ManagementTicket

  // Method 2 using deployment config
  val altControlAwareActor = system.actorOf(Props[SimpleActor], "altControlActorAware")

  altControlAwareActor ! "[P2] this thing needs to be solved now"
  altControlAwareActor ! "[P1] this thing can be solved when you have time"
  altControlAwareActor ! "[P3] this thing would be nice to have!"
  altControlAwareActor ! ManagementTicket


}


