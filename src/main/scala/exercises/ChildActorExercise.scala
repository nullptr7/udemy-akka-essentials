package exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorExercise extends App {

  // Distributed word counting -- implementing

  object WordCounterMaster {

    case class Initialize(nChildren: Int)

    case class WordCountTask(id: Int, text: String)

    case class WordCountReply(id: Int, count: Int)

  }

  class WordCounterMaster extends Actor {

    import WordCounterMaster._

    override def receive: Receive = {
      case Initialize(nChildren) =>
        println(s"[master] - Initializing $nChildren children...")
        val numberOfWorkers: Seq[ActorRef] =
          for (n <- 1 to nChildren) yield context.actorOf(Props[WordCounterWorker], s"worker_$n")
        /*
          Initializing things for the children to handle the tasks along with number of children
         */
        context.become(withChildren(numberOfWorkers, 0, 0, Map()))
    }

    def withChildren(childRefs: Seq[ActorRef], currentChildIndex: Int, currentTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case text: String =>
        println(s"[master] I have received `$text` and I will send it to child ($currentChildIndex)")
        val wordCountTask = WordCountTask(currentTaskId, text)
        val childRef = childRefs(currentChildIndex)
        childRef ! wordCountTask
        /*
            Control will go the next child or the first child if the noOfChild have reached
         */
        val newChildIndex = (currentChildIndex + 1) % childRefs.length
        val newTaskId = currentTaskId + 1
        val newRequestMap: Map[Int, ActorRef] = requestMap + (currentTaskId -> sender())
        context.become(withChildren(childRefs, newChildIndex, newTaskId, newRequestMap))
      case WordCountReply(id, count) =>
        println(s"[master] I have received reply of $id and number of words $count")
        // Here the problem is who is the sender??
        // sender() is incorrect because it is the last actorRef that has send me the the reply
        // Hence if we use sender() it will be incorrect.! It will be the child who sent me this in this case
        // To handle this we have to add an Identification to the children who is doing our work
        // Hence we update the class as WordCountTask(id: Int, task: String) & WordCountReply(id: Int, count: Int)
        val originalSender = requestMap(id)
        originalSender ! count
        context.become(withChildren(childRefs, currentChildIndex, currentTaskId, requestMap - id))
    }
  }

  class WordCounterWorker extends Actor {

    import WordCounterMaster._

    override def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"[child] ${self.path} I have received task $id with text $text")
        sender() ! WordCountReply(id, text.split(" ").length)
    }
  }

  /*
    create wordCounterMaster
    send initialize(10) to wordCounterMaster
    after it has initialized
    send "akka is awesome" to wordCounterMaster
      wCM will send a wordCounterTask("...") to one of its children
      child(wCW) will reply with a wordCountReply(3) 'akka is awesome' has 3 to the master
      and the master in  turn replies the number 3 to the sender

    requester -> wCM -> wCW
            r <- wCM <- wCW (replies)

    Use roundRobin logic to implement if tasks > noOfChildren

   */

  class TestActor extends Actor {

    import WordCounterMaster.Initialize

    override def receive: Receive = {
      case "go" =>
        val master = context.actorOf(Props[WordCounterMaster], "master")
        master ! Initialize(3)
        val text =
          List("I love scala", "Akka is dope",
            "I am learning Akka fundamentals",
            "next is graphQL",
            "And finally sangria",
            "This is another test message",
            "One more cz I am getting bored!")
        text.foreach(master ! _)
      case count: Int =>
        println(s"[testActor] value received is $count")

    }
  }

  val sys = ActorSystem("roundRobinExercise")
  val testActor = sys.actorOf(Props[TestActor])
  testActor ! "go"

}