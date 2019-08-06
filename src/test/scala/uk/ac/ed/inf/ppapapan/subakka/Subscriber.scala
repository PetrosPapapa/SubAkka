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

class SubscriberTests extends TestKit(ActorSystem("SubscriberTests", ConfigFactory.parseString(MockPublisher.config))) with
    WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender {

  //implicit val timeout:FiniteDuration = 10.seconds
  override def beforeAll:Unit = {
    system.eventStream.setLogLevel(Logging.DebugLevel)
  }
  override def afterAll:Unit = {
    println("Shutting down...")
    TestKit.shutdownActorSystem(system)
  }

  "The Subscriber" must {

    "publish events to 3 observers" in {
      val p = MockPublisher.actor(system,ME1,ME1,MEX)

      val f1 = MockSubscriber.sub(p,self)
      val f2 = MockSubscriber.sub(p,self)
      val f3 = MockSubscriber.sub(p,self)

      for (i <- 1 to 3) {
        expectMsg(70.seconds, SOK)
      }
      p ! MockPublisher.Publish

      Await.result(f1, 1.seconds) should be (3)
      Await.result(f2, 1.seconds) should be (3)
      Await.result(f3, 1.seconds) should be (3)
    }

    "publish events to many observers" in {
      val n = 9000

      val probe = TestProbe()

      val p = MockPublisher.actor(system,ME3,ME1,ME1,ME1,ME1,ME1,ME1,ME1,ME1,MEX)

      val q = scala.collection.mutable.Queue[Future[Int]]()

      for (i <- 1 to n) {
        q += MockSubscriber.sub(p,self)
      }

      for (i <- 1 to n) {
        expectMsg(70.seconds, SOK)
      }
      p ! MockPublisher.Publish

      q.map { f => f.onComplete { t => {
        t.getOrElse(0) should be (10)
        probe.ref ! OK
      }
      }(system.dispatcher) }

      for (i <- 1 to n) {
        probe.expectMsg(50.seconds, OK)
      }
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
  def sub(publisher: ActorRef, ack: ActorRef)(implicit system: ActorSystem): Future[Int] = {
    val s = new MockSubscriber()
    implicit val tOut = Timeout(1.minute)
    val a = system.actorOf(Props(new SubscriberActor(s)))
    val f = a ? Subscriber.SubAndForgetTo(publisher)
    f.onComplete { _ => 
      println(s"[$a] Subscribed!")
      ack ! SOK
    }(system.dispatcher)
//    Await.result(s.subAndForgetTo(publisher,None,30.seconds),1.minute)
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

case object OK
case object SOK
