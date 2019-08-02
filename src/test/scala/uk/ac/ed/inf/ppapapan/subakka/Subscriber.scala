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

class SubscriberTests extends TestKit(ActorSystem("SubscriberTests", ConfigFactory.parseString(MockPublisher.config))) with
    WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender {

  //implicit val timeout:FiniteDuration = 10.seconds
  override def beforeAll:Unit = {
    system.eventStream.setLogLevel(Logging.DebugLevel)
  }
  override def afterAll:Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "The Subscriber" must {

    "publish events to 3 observers" in {
      val p = MockPublisher.actor(system,ME1,ME1,ME2)

      val f1 = MockSubscriber.sub(p)
      val f2 = MockSubscriber.sub(p)
      val f3 = MockSubscriber.sub(p)

      //Thread.sleep(1000)
      p ! MockPublisher.Publish

      Await.result(f1, 1.seconds) should be (3)
      Await.result(f2, 1.seconds) should be (3)
      Await.result(f3, 1.seconds) should be (3)
    }
  }
}

class MockSubscriber extends Subscriber[MockEvent] {
  var count = 0
  val promise = Promise[Int]()
  def future = promise.future

  override def onInit(publisher: ActorRef): Unit = count = 0
  override def onEvent(event: MockEvent): Unit = count += 1
  override def onDone(publisher: ActorRef, subscriber: ActorRef): Unit = promise.success(count)
  override def onFail(e: Throwable, publisher: ActorRef, subscriber: ActorRef): Unit = promise.failure(e) 
}

object MockSubscriber {
  def sub(publisher: ActorRef)(implicit system: ActorSystem): Future[Int] = {
    val s = new MockSubscriber()
    Await.result(s.subAndForgetTo(publisher),3.seconds)
    s.future
  }

  def probe(publisher: ActorRef)(implicit system: ActorSystem) = {
    val probe = TestProbe()

    probe.send(publisher, Publisher.SubAndForget(None))
    probe.expectMsgType[Publisher.StreamInitAndForget]
    probe.reply(Publisher.StreamAck)
    println("Probe ready!")
    probe
  }
}
