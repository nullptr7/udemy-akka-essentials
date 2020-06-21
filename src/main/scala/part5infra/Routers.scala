package part5infra

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.routing._
import com.typesafe.config.ConfigFactory

object Routers extends App {

  // Master will route all the request to child

  /**
   * Somehow route all the messages to children
   *
   * #1 Manual router
   */
  class Master extends Actor {
    // Step1 create routees
    // 5 actor routees based off slave actors
    private val slaves = for (slaveIndex <- 1 to 5) yield {
      val slave: ActorRef = context.actorOf(Props[Slave], s"slave_$slaveIndex")
      context.watch(slave)
      ActorRefRoutee(slave) // TODO: Work in progress
    }

    // Step2 define the router
    private val router = Router(RoundRobinRoutingLogic(), slaves)

    override def receive: Receive = {
      // Step 4 - handle the termination/lifecycle of the routees
      case Terminated(ref) =>
        // Replace the terminated slave with the new one
        router.removeRoutee(ref)
        val newSlave: ActorRef = context.actorOf(Props[Slave])
        context.watch(newSlave)
        router.addRoutee(newSlave)
      // Step 3 - route the messages
      case message =>
        // sender() is the requester of the message
        // the slave actors can reply to the message without involving the master
        router.route(message, sender())
    }
  }

  // Does most of the work
  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("RouterDemo" /*TODO*/)
  val master = system.actorOf(Props[Master])

  /**
   * Method #2.1 - Router actor with its own children
   * Pool Router
   */
  // In this case, router has its own children
  // Method 1 we have to write many thing, but in this case it works just fine with Method 2.1
  //  val poolMaster: ActorRef = system.actorOf(RoundRobinPool(5).props(Props[Slave]), "SimplePoolMaster")
  //
  //  for (i <- 1 to 10) poolMaster ! s"Hello from poolMaster sending wishes to $i"

  // Method 2.2 we can do from configuration
  /*
    # routers demo
    routerDemo {
      akka {
        actor.deployment {
          # name of the actor '/' means sub-ordinate of user guardian
          /poolMaster2 {
            router = round-robin-pool
            nr-of-instances = 5
          }
        }
      }
    }
   */
  val systemWithRoutersDemo = ActorSystem("RouterDemoWithConfig", ConfigFactory.load().getConfig("routerDemo"))
  val poolMaster2 = systemWithRoutersDemo.actorOf(FromConfig.props(Props[Slave]), "poolMaster2")

  //  for (index <- 1 to 10) master ! s"[$index] Hello from the world"

  //  for (i <- 1 to 10) poolMaster2 ! s"Hello from poolMaster2 sending wishes to $i"

  // Method 3 - Router with actors created elsewhere
  // Also known as groupRouter
  // .. assume an actor in another part of the application
  val slaveList = (1 to 5).map(i => system.actorOf(Props[Slave], s"slave_$i")).toList

  // need their paths since they are created elsewhere
  val slavePaths = slaveList.map(_.path.toString)

  // 3.1 we need only props since slaves are already created
  val groupMaster = system.actorOf(RoundRobinGroup(slavePaths).props())
  //    for (i <- 1 to 10) groupMaster ! s"Hello from groupMaster sending wishes to $i"

  // 3.2 configuration
  val groupMaster2 = systemWithRoutersDemo.actorOf(FromConfig.props(Props[Slave]), "groupMaster2")
  for (i <- 1 to 10) groupMaster2 ! s"Hello from groupMaster2 sending wishes to $i"

  /*
      handling of special messages
  */
  //  groupMaster ! Broadcast("Hello, everyone")
}
