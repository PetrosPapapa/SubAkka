package uk.ac.ed.inf.ppapapan.subakka

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.event.{ Logging, LoggingReceive }
import akka.stream.KillSwitch
import akka.testkit.{ ImplicitSender, TestActor, TestActors, TestKit, TestProbe }
import com.typesafe.config.ConfigFactory
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }
import org.scalatest.junit.JUnitRunner
import scala.concurrent.{ Await, Future, Promise }
import scala.concurrent.duration._
import akka.pattern.{ask, pipe}
import scala.concurrent.ExecutionContext
import akka.util.Timeout
import scala.util.Success

class SubscriberTests extends TestKit(ActorSystem("SubscriberTests")) with
    WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender {

  override def beforeAll:Unit = {
    system.eventStream.setLogLevel(Logging.DebugLevel)
  }
  override def afterAll:Unit = {
    println("Shutting down...")
    TestKit.shutdownActorSystem(system)
  }

  "The Subscriber" must {

    "publish events to 3 observers" in {
      val p = system.actorOf(Props(new MockPublisher(E1,E2,EX)))

      val f1 = MockSubscriber.sub(p,self)
      val f2 = MockSubscriber.sub(p,self)
      val f3 = MockSubscriber.sub(p,self)

      for (i <- 1 to 3) {
        expectMsgType[Publisher.StreamInit](70.seconds)
      }
      p ! MockPublish

      Await.result(f1, 1.seconds) should be (3)
      Await.result(f2, 1.seconds) should be (3)
      Await.result(f3, 1.seconds) should be (3)
    }

    "publish events to many observers" in {
      val n = 90000

      val p = system.actorOf(Props(new MockPublisher(E1,E2,E3,E4,E5,EX)))

      val q = scala.collection.mutable.Queue[Future[Int]]()

      for (i <- 1 to n) {
        q += MockSubscriber.sub(p,self)
      }

      for (i <- 1 to n) {
        expectMsgType[Publisher.StreamInit](70.seconds)
      }
      p ! MockPublish

      q.map { f => Await.result(f, 10.seconds) should be (6) }
    }
  }
}

class MockSubscriber extends Subscriber {
  var count = 0
  val promise = Promise[Int]()
  def future = promise.future

  override def onInit(publisher: ActorRef): Unit = count = 0
  override def onEvent(event: Event): Unit = count += 1
  override def onDone(publisher: ActorRef, subscriber: ActorRef): Unit = promise.success(count)
  override def onFail(e: Throwable, publisher: ActorRef, subscriber: ActorRef): Unit = promise.failure(e) 
}

object MockSubscriber {
  def sub(publisher: ActorRef, ack: ActorRef)(implicit system: ActorSystem): Future[Int] = {
    val s = new MockSubscriber()
    implicit val tOut = Timeout(1.minute)
    val a = system.actorOf(Props(new SubscriberActor(s)))

    val f = publisher ! Publisher.Subscribe(a, Some(ack))

    s.future
  }
}

case object OK
