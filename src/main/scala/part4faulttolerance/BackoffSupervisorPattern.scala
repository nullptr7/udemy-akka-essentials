package part4faulttolerance

import java.io.File

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{Backoff, BackoffOnFailureOptions, BackoffOpts, BackoffSupervisor}

import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps

object BackoffSupervisorPattern extends App {

  case object ReadFile

  class FileBasedPersistentSupervisor extends Actor with ActorLogging {

    var dataSource: Source = _

    override def preStart: Unit = log.info("Persistent Actor starting...")

    override def postStop: Unit = log.warning("Persistent actor has stopped!")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.warning("Persistent actor restarting...")

    override def receive: Receive = {
      case ReadFile =>
        if (dataSource == null)
          dataSource = Source.fromFile(new File("src/main/resources/json/testfile1/important.txt"))
        log.info("I have just read some important data: " + dataSource.getLines().toList)
    }
  }

  val system = ActorSystem("BackoffSupervisorPattern")
  //  val simpleActor = system.actorOf(Props[FileBasedPersistentDoctor], "simpleActor")
  //  simpleActor ! ReadFile

  private val options = Backoff.onFailure(
    Props[FileBasedPersistentSupervisor],
    "simpleActor",
    3 seconds,
    30 seconds,
    0.2
  )
  val supervisorBackOffProps: Props = BackoffSupervisor.props(options)

  // Alternative, though need to understand how it works
  val supervisorBackOffPropsNonDeprecated: BackoffOnFailureOptions = BackoffOpts.onFailure(
    Props[FileBasedPersistentSupervisor],
    "simpleActor",
    1 seconds, // 3 seconds, 6 seconds, 12 seconds, 24 seconds and done
    30 seconds,
    0.2 // helps to avoid starting actors at that exact moment
  )

  //  val simpleSupervisorBackoffSupervisor = system.actorOf(supervisorBackOffProps, "simpleSupervisor")
  //
  //  simpleSupervisorBackoffSupervisor ! ReadFile

  //  private val nonDeprecatedProps: Props = BackoffSupervisor.props(supervisorBackOffPropsNonDeprecated)
  //
  //  val simpleSupervisorBackoffSupervisorNonDeprecated =
  //    system.actorOf(nonDeprecatedProps, "UsingNonDeprecatedOne")
  //
  //  simpleSupervisorBackoffSupervisorNonDeprecated ! ReadFile

  /*
      simpleSupervisor
       - Child called simpleBackOffActor of props of type FileBasedPersistentDoctor
       - Supervision strategy is the default one
        - first attempt kicks in 3 seconds
        - next attempt will be 2x the previous attempt
   */

  val stopSupervisorProps = BackoffSupervisor.props(
    Backoff.onStop(
      Props[FileBasedPersistentSupervisor],
      "stopBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    ).withSupervisorStrategy(
      OneForOneStrategy() {
        case _ => Stop
      }
    )
  )

  val stopSupervisor = system.actorOf(stopSupervisorProps, "stopSupervisor")

  stopSupervisor ! ReadFile
}
