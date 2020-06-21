package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}

object StartStoppingActors extends App {

  val system = ActorSystem("StoppingActorDemo")

  object Parent {

    case class StartChild(name: String)

    case class StopChild(name: String)

    case object Stop

  }

  class Parent extends Actor with ActorLogging {
    override def receive: PartialFunction[Any, Unit] = {
      withChildren(Map.empty)
    }

    import Parent._

    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info(s"starting child with a name $name")
        context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))
      case StopChild(name) =>
        log.info(s"stopping child with name $name")
        val childOption = children get name
        childOption.foreach(context.stop)
        log.info("Stopping child...")
      case Stop =>
        log.info("stopping myself...")
        context.stop(self)

    }
  }

  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message: String => log.info(message)
    }
  }

  /**
   * Can be stopped by two methods
   *
   * Method #1 - Using context.stop
   */

  import Parent._

  val parent = system.actorOf(Props[Parent], "parent")
  parent ! StartChild("child1")
  val child = system.actorSelection("/user/parent/child1")
  child ! "Hi, kid"
  //parent ! StopChild("child1")
  //  Thread.sleep(2000)

  parent ! StartChild("child2")
  val child2 = system.actorSelection("/user/parent/child2")
  //  Thread.sleep(2000)
  //  parent ! Stop
  //  for (i <- 1 to 10) parent ! "parent are you still there?"
  //  for (i <- 1 to 10) child2 ! s"checking if the parent is alive $i"
  //  Thread.sleep(2000)

  // Method #2 - via special messages
  val looseActor = system.actorOf(Props[Child])

  looseActor ! "Hello Loose Actor!"
  looseActor ! PoisonPill

  //  for (_ <- 1 to 3) looseActor ! "Are you there! from looseActor"

  val abruptlyTerminateActor = system.actorOf(Props[Child])
  abruptlyTerminateActor ! "You are about to be terminated!"
  abruptlyTerminateActor ! Kill
  abruptlyTerminateActor ! "You are terminated!"

  // Method 3 - Death watch, monitors when actors dies

  class Watcher extends Actor with ActorLogging {

    import Parent._

    override def receive: Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"Started watching child $name")
        context.watch(child)
      case Terminated(ref) =>
        log.info(s"The reference that I am watching is terminated $ref has stopped!")
    }
  }

  val watcher = system.actorOf(Props[Watcher], "watcherDemo")
  watcher ! StartChild("WatchedChild")
  val watchedChild = system.actorSelection("/user/watcherDemo/WatchedChild")
  Thread.sleep(500)
  watchedChild ! Kill

}
