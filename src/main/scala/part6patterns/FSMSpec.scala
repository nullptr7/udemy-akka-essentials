package part6patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, FSM, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContextExecutor
import scala.language.postfixOps

class FSMSpec extends TestKit(ActorSystem("FSMSpec")) with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import FSMSpec._

  "A vending machine" should {
    "error when not initialized" in {
      val vendingMachine = system.actorOf(Props[VendingMachine])
      vendingMachine ! RequestProduct("coke")
      expectMsg(VendingError("VendingMachineNotInitialized"))
    }

    "report a product not available" in {
      val vendingMachine = system.actorOf(Props[VendingMachine])
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 1))
      vendingMachine ! RequestProduct("pepsi")
      expectMsg(VendingError("ProductNotFound"))

    }

    "throw a timeout if I do not insert money" in {
      val vendingMachine = system.actorOf(Props[VendingMachine])
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 1))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert $ 1"))
      import scala.concurrent.duration._
      within(1.5 seconds) {
        expectMsg(VendingError("RequestTimedOut"))
      }

    }

    "should send the money" in {
      val vendingMachine = system.actorOf(Props[VendingMachine])
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 3))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert $ 3"))
      vendingMachine ! ReceiveMoney(1)
      expectMsg(Instruction("Please enter remaining money $2"))
      import scala.concurrent.duration._
      within(1.5 seconds) {
        expectMsg(VendingError("RequestTimedOut"))
        expectMsg(GiveBackChange(1))
      }
    }

    "deliver all the product if all money is inserted" in {
      val vendingMachine = system.actorOf(Props[VendingMachine])
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 3))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert $ 3"))
      vendingMachine ! ReceiveMoney(3)
      expectMsg(Deliver("coke"))
    }

    "give back and able to request money for a new product" in {
      val vendingMachine = system.actorOf(Props[VendingMachine])
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 3))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert $ 3"))

      vendingMachine ! ReceiveMoney(4)
      expectMsg(Deliver("coke"))
      expectMsg(GiveBackChange(1))

      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert $ 3"))

    }


    "error when not initialized VendingMachineFSM" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])
      vendingMachine ! RequestProduct("coke")
      expectMsg(VendingError("MachineNotInitializedError"))
    }

    "report a product not available VendingMachineFSM" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 1))
      vendingMachine ! RequestProduct("pepsi")
      expectMsg(VendingError("ProductNotAvailable"))

    }

    "throw a timeout if I do not insert money VendingMachineFSM" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 1))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert $1"))
      import scala.concurrent.duration._
      within(1.5 seconds) {
        expectMsg(VendingError("RequestTimedOut"))
      }

    }

    "should send the money VendingMachineFSM" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 3))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert $3"))
      vendingMachine ! ReceiveMoney(1)
      expectMsg(Instruction("Please enter remaining money $2"))
      import scala.concurrent.duration._
      within(1.5 seconds) {
        expectMsg(VendingError("RequestTimedOut"))
        expectMsg(GiveBackChange(1))
      }
    }

    "deliver all the product if all money is inserted VendingMachineFSM" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 3))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert $3"))
      vendingMachine ! ReceiveMoney(3)
      expectMsg(Deliver("coke"))
    }

    "give back and able to request money for a new product VendingMachineFSM" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 3))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert $3"))

      vendingMachine ! ReceiveMoney(4)
      expectMsg(Deliver("coke"))
      expectMsg(GiveBackChange(1))

      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert $3"))

    }
  }

}

object FSMSpec {

  /*
      Vending machine
   */

  case class Initialize(inventory: Map[String, Int], price: Map[String, Int])

  case class RequestProduct(product: String)

  case class Instruction(instruction: String)

  case class ReceiveMoney(amount: Int)

  case class Deliver(product: String)

  case class GiveBackChange(amount: Int)

  case class VendingError(reason: String)

  case object ReceiveMoneyTimeout

  class VendingMachine extends Actor with ActorLogging {
    override def receive: Receive = idle

    def idle: Receive = {
      case Initialize(inventory, price) =>
        log.info("Initializing vending machine...")
        context.become(operational(inventory, price))
      case _ => sender() ! VendingError("VendingMachineNotInitialized")
    }

    def operational(inventory: Map[String, Int], prices: Map[String, Int]): Receive = {
      case RequestProduct(product) => inventory.get(product) match {
        case None | Some(0) => sender() ! VendingError("ProductNotFound")
        case Some(_) =>
          log.info("fetching product")
          val price: Int = prices(product)
          log.info(s"price of the $product is $$$price")
          sender() ! Instruction(s"Please insert $$ $price")
          context.become(waitForMoney(inventory, prices, product, 0, startReceiveMoneyTimeoutSchedule, sender()))
      }
      case GiveBackChange(amount) =>
        log.info(s"Here is your change of $$$amount")
    }

    def waitForMoney(inventory: Map[String, Int], prices: Map[String, Int],
                     product: String, moneyRequiredToGetProduct: Int,
                     timeoutSchedule: Cancellable, requester: ActorRef): Receive = {

      case ReceiveMoneyTimeout =>
        requester ! VendingError("RequestTimedOut")
        if (moneyRequiredToGetProduct > 0) requester ! GiveBackChange(moneyRequiredToGetProduct)
        context.become(operational(inventory, prices))

      case ReceiveMoney(amountEnteredByUser) =>
        log.info(s"money entered $moneyRequiredToGetProduct")
        timeoutSchedule.cancel()
        val priceOfProduct = prices(product)
        if (moneyRequiredToGetProduct + amountEnteredByUser >= priceOfProduct) {
          log.info("moneyRequiredToGetThisProduct - {} and amountEnteredByUser - {} and priceOfProduct - {}",
            moneyRequiredToGetProduct, amountEnteredByUser, priceOfProduct)
          // User buys the product
          requester ! Deliver(product)

          // Deliver the change
          if (moneyRequiredToGetProduct + amountEnteredByUser - priceOfProduct > 0) {
            log.info("giving money back")
            requester ! GiveBackChange(moneyRequiredToGetProduct + amountEnteredByUser - priceOfProduct)
          }

          // updating inventory
          val newStock = inventory(product) - 1
          val newInventory = inventory + (product -> newStock)
          context.become(operational(newInventory, prices))
        } else {
          val remainingMoney = priceOfProduct - moneyRequiredToGetProduct - amountEnteredByUser
          requester ! Instruction(s"Please enter remaining money $$$remainingMoney")
          context.become(waitForMoney(inventory, prices, product, moneyRequiredToGetProduct + amountEnteredByUser, startReceiveMoneyTimeoutSchedule, requester))
        }
    }

    import scala.concurrent.duration._

    implicit val executionContext: ExecutionContextExecutor = context.dispatcher

    private def startReceiveMoneyTimeoutSchedule: Cancellable = context.system.scheduler.scheduleOnce(1 second) {
      self ! ReceiveMoneyTimeout
    }
  }

  /*
    event => consist of data and state, it can be changed at any event

    as per code below

    Initial stage
    state = Idle
    data = UnInitialized

    event(Initialize(inventory, prices))
    event(Initialize(Map(coke -> 10), Map(coke -> 1)))
      =>
        state = Operational
        data = Initialized(inventory, prices)

    event(RequestProduct(coke))
      =>
        state = WaitForMoney
        data = WaitForMoneyData(Map(coke -> 10), Map(coke -> 1), coke, 0, R)

    event(ReceiveMoney(2))
      =>
        state = Operational
        data = Initialized(Map(code -> 9), Map(coke -> 1)
   */

  // Step 1 - define the state and the data of the actor
  trait VendingState

  case object Idle extends VendingState

  case object Operational extends VendingState

  case object WaitForMoney extends VendingState

  trait VendingData

  case object UnInitialized extends VendingData

  case class Initialized(inventory: Map[String, Int], prices: Map[String, Int]) extends VendingData

  case class WaitForMoneyData(inventory: Map[String, Int], prices: Map[String, Int],
                              product: String, moneyRequiredToGetProduct: Int,
                              requester: ActorRef) extends VendingData

  class VendingMachineFSM extends FSM[VendingState, VendingData] {

    // we don't have a receive handler

    // when an FSM receives a message, it triggers an event, the event contains the message and the data that is currently on hold for this FSM
    startWith(Idle, UnInitialized)

    when(Idle) {
      case Event(Initialize(inventory, prices), UnInitialized) =>
        goto(Operational) using Initialized(inventory, prices)
      // Similar to context.become(operational(inventory, prices))
      case _ =>
        sender() ! VendingError("MachineNotInitializedError")
        stay()
    }

    when(Operational) {
      case Event(RequestProduct(product), Initialized(inventory, prices)) =>
        inventory.get(product) match {
          case None | Some(0) => sender() ! VendingError("ProductNotAvailable")
            stay()

          case Some(_) =>
            val price: Int = prices(product)
            val requester = sender()
            requester ! Instruction(s"Please insert $$$price")
            goto(WaitForMoney) using
              WaitForMoneyData(inventory, prices, product, 0, requester)
        }
    }

    import scala.concurrent.duration._

    when(WaitForMoney, stateTimeout = 1 second) {
      case Event(StateTimeout, WaitForMoneyData(inventory, prices, product, moneyRequiredToGetProduct, requester)) =>
        requester ! VendingError("RequestTimedOut")
        if (moneyRequiredToGetProduct > 0) requester ! GiveBackChange(moneyRequiredToGetProduct)
        goto(Operational) using Initialized(inventory, prices)
      case Event(ReceiveMoney(amountEnteredByUser), WaitForMoneyData(inventory, prices, product, moneyRequiredToGetProduct, requester)) =>
        val priceOfProduct = prices(product)
        if (moneyRequiredToGetProduct + amountEnteredByUser >= priceOfProduct) {
          // User buys the product
          requester ! Deliver(product)

          // Deliver the change
          if (moneyRequiredToGetProduct + amountEnteredByUser - priceOfProduct > 0) {
            log.info("giving money back")
            requester ! GiveBackChange(moneyRequiredToGetProduct + amountEnteredByUser - priceOfProduct)
          }

          // updating inventory
          val newStock = inventory(product) - 1
          val newInventory = inventory + (product -> newStock)
          goto(Operational) using Initialized(newInventory, prices)
        } else {
          val remainingMoney = priceOfProduct - moneyRequiredToGetProduct - amountEnteredByUser
          requester ! Instruction(s"Please enter remaining money $$$remainingMoney")
          stay() using WaitForMoneyData(inventory, prices, product, moneyRequiredToGetProduct + amountEnteredByUser, requester)
        }
    }

    whenUnhandled {
      case Event(_, _) =>
        sender() ! VendingError("CommandNotFound")
        stay()
    }

    onTransition {
      case stateA -> stateB => log.info(s"transitioning from $stateA to $stateB")
    }

    initialize()
  }

}
