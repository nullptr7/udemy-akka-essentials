package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntroAkkaConfig extends App {

  class SimpleLoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {

      case message: String => log.info(message)
    }
  }

  // Method #1 Inline configuration

  val configurationString =
    """
      |akka {
      | loglevel = "ERROR"
      |}
      |""".stripMargin

  val config = ConfigFactory.parseString(configurationString)
  val system = ActorSystem("Configuration", ConfigFactory.load(config))
  val simpleLoggingActor = system.actorOf(Props[SimpleLoggingActor])

  simpleLoggingActor ! "message to remember"

  // Method #2 Configuration in a file

  val defaultConfigurationFileSystem = ActorSystem("DefaultConfigFileDemo")
  val defaultActor = defaultConfigurationFileSystem.actorOf(Props[SimpleLoggingActor])

  defaultActor ! "Remember Me!"

  /**
   * Method # 3 - separate configuration in separate file
   */

  val specialConfigObject = ConfigFactory.load().getConfig("mySpecialConfig")
  val specialActorSystem = ActorSystem("SpecialActorSystem", specialConfigObject)
  val specialActor = specialActorSystem.actorOf(Props[SimpleLoggingActor])

  specialActor ! "Remember me, I am special!"

  /*
    Method #4 - In a separate config file
   */

  val separateConfig = ConfigFactory.load("secretfolder/secretConfiguration.conf")
  println(s"separate config log level: ${separateConfig.getString("akka.loglevel")}")
  val jsonConfig = ConfigFactory.load("json/jsonConfig.json")
  println(s"jsonConfig config log level: ${jsonConfig.getString("akka.loglevel")}")
  println(s"jsonConfig config log level: ${jsonConfig.getString("aJsonProperty")}")

  /*
      Method # 5 different file format JSON, YML etc..
   */
}
