package part6patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

class AskSpec extends TestKit(ActorSystem("AskSpec")) with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

  import part6patterns.AskSpec.AuthManager._

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import AskSpec._

  "An authenticator" should {
    "fail to authenticate non-registered user" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! Authenticate("Daniel", "rockthejvm")
      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }

    "fail to authenticate if invalid password" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! RegisterUser("Daniel", "rtjvm")
      authManager ! Authenticate("Daniel", "rockthejvm")
      expectMsg(AuthFailure(AUTH_FAILURE_PASS_INCORRECT))

    }

    "successfully authenticate the user" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! RegisterUser("Daniel", "rtjvm")
      authManager ! Authenticate("Daniel", "rtjvm")
      expectMsg(AuthSuccess)
    }

    "fail to authenticate non-registered user via PipeAuthManager" in {
      val pipedAuthManager = system.actorOf(Props[PipedAuthManager])
      pipedAuthManager ! Authenticate("Daniel", "rockthejvm")
      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }

    "fail to authenticate if invalid password via PipedAuthManager" in {
      val pipedAuthManager = system.actorOf(Props[PipedAuthManager])
      pipedAuthManager ! RegisterUser("Daniel", "rtjvm")
      pipedAuthManager ! Authenticate("Daniel", "rockthejvm")
      expectMsg(AuthFailure(AUTH_FAILURE_PASS_INCORRECT))

    }

    "successfully authenticate the user via PipedAuthManager" in {
      val pipedAuthManager = system.actorOf(Props[PipedAuthManager])
      pipedAuthManager ! RegisterUser("Daniel", "rtjvm")
      pipedAuthManager ! Authenticate("Daniel", "rtjvm")
      expectMsg(AuthSuccess)
    }
  }

}

object AskSpec {

  // Assume this code is written somewhere else.

  case class Read(key: String)

  case class Write(key: String, value: String)

  class KVActor extends Actor with ActorLogging {

    override def receive: Receive = online(Map.empty)

    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"trying to read the value key $key")
        sender() ! kv.get(key) // returns Option[String]

      case Write(key, value) =>
        log.info(s"writing value $value to key $key")
        context.become(online(kv + (key -> value)))
    }
  }

  // We will use KVActor by the UserAuthenticatorActor
  case class RegisterUser(username: String, password: String)

  case class Authenticate(username: String, password: String)

  case class AuthFailure(msg: String)

  case object AuthSuccess

  // Step 1

  import akka.pattern.ask
  import scala.concurrent.duration._

  object AuthManager {
    val AUTH_FAILURE_NOT_FOUND = "username not found"
    val AUTH_FAILURE_PASS_INCORRECT = "password incorrect"
    val AUTH_FAILURE_SYSTEM = "system error"
  }

  class AuthManager extends Actor with ActorLogging {

    // Step 2: logistics

    implicit val timeout: Timeout = Timeout(1 second)
    implicit val executionContext: ExecutionContext = context.dispatcher

    protected val authDb: ActorRef = context.actorOf(Props[KVActor])

    import AuthManager._

    override def receive: Receive = {
      case RegisterUser(username, password) => authDb ! Write(username, password)

      case Authenticate(username, password) => handleAuthentication(username, password)
      // context.become(waitingForPassword(username, sender()))


    }

    def handleAuthentication(username: String, password: String): Unit = {
      // step3 - ask the actor
      val future = authDb ? Read(username)
      val originalSender = sender()
      // step4 - handle the future for e.g. with onComplete
      // NEVER USE CALL METHOD ON ACTOR INSTANCES OR ACCESS MUTABLE STATE IN ON-COMPLETE METHOD
      // Since it is future
      // Avoid closing over the actor instance or mutable state
      future.onComplete {
        case Success(None) => originalSender ! AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Success(Some(dbPassword)) =>
          if (dbPassword == password) originalSender ! AuthSuccess else originalSender ! AuthFailure(AUTH_FAILURE_PASS_INCORRECT)

        case Failure(_) => originalSender ! AuthFailure(AUTH_FAILURE_SYSTEM)
      }
    }

    // Below is not a good use-case since this logic would be complicated if we are in distributed environment where this
    // actor will be bombarded with the messages

    def waitingForPassword(str: String, ref: ActorRef): Receive = {
      case password: Option[String] => ??? //do password check
    }
  }

  class PipedAuthManager extends AuthManager {
    override def handleAuthentication(username: String, password: String): Unit = {
      // step 3 - ask the actor
      val future: Future[Any] = authDb ? Read(username) // Type is future[Any]
      // Step 4 - process the future until you get the response you will send back
      import AuthManager._
      val passwordFuture: Future[Option[String]] = future.mapTo[Option[String]] // Type is Future[Option[String]]
      val responseFuture = passwordFuture.map {
        case None => AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Some(dbPassword) =>
          if (dbPassword == password) AuthSuccess else AuthFailure(AUTH_FAILURE_PASS_INCORRECT)
      } // this will be Future[Any] - will be completed with the response I will send back

      import akka.pattern.pipe
      /*
          When the future completes, send the response to the actorRef in the arg list
          so when responseFuture is complete it will send whatever it gets to sender() or any actorRef we mention
       */
      // Step 5 - pipe the resulting future to the actor you want the result to be sent to
      responseFuture.pipeTo(sender())
    }
  }

}
