package part2actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorIntro extends App {

  // part1 - actor systems
  val actorSystem = ActorSystem("firstActorSystem")

  println(actorSystem.name)

  // part2 - create actors
  // word count actor

  type IReceive = PartialFunction[Any, Unit]

  class WordCountActor extends Actor {
    var totalWords = 0

    def receive: IReceive = {
      case message: String =>
        totalWords += message.split(" ").length
        println(s"[word counter] I have received message of size $totalWords")
      case msg => println(s"[word counter] I cannot understand ${msg.toString}")
    }


    /*
        override def receive: Receive = {
          case message: String => totalWords += message.split(" ").length
          case msg => println(s"[word counter] I cannot understand ${msg.toString}")
        }
    */

    /*
        def receive: PartialFunction[Any, Unit] = {
          case message: String => totalWords += message.split(" ").length
          case msg => println(s"[word counter] I cannot understand ${msg.toString}")
        }
    */
  }

  // Part 3, instantiate or actor
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  val anotherWordCounter = actorSystem.actorOf(Props[WordCountActor], "anotherWordCounter")

  // Part4 communicate
  wordCounter ! "I am learning Akka and it is cool!"
  anotherWordCounter ! "I am another word counter..."

  // ASync messages!

  // Best practice if we are passing instance to Props object
  object Person {
    def props(name: String): Props = Props(new Person(name))
  }

  class Person(name: String) extends Actor {
    override def receive: IReceive = {
      case "hi" => println(s"My name is $name")
      case _ => println("I do not what you mean!")
    }
  }

  val personActor = actorSystem.actorOf(Props(new Person("Bob")), "personActor")
  val personActorWithCompanionBestPractice = actorSystem.actorOf(Person.props("Tom"), "personActorWithCompanionBestPractice")
  personActor ! "test"
  personActorWithCompanionBestPractice ! "hi"
}
