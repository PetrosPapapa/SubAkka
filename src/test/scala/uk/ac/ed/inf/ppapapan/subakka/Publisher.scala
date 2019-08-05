package uk.ac.ed.inf.ppapapan.subakka

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.event.{ Logging, LoggingReceive }
import akka.testkit.{ ImplicitSender, TestActor, TestActors, TestKit, TestProbe }
import com.typesafe.config.ConfigFactory
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }
import org.scalatest.junit.JUnitRunner
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import akka.pattern.{ask, pipe}
import scala.concurrent.ExecutionContext
import akka.util.Timeout

class PublisherTests extends TestKit(ActorSystem("PublisherTests")) with
    WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender {

  override def beforeAll:Unit = {
    system.eventStream.setLogLevel(Logging.DebugLevel)
  }
  override def afterAll:Unit = {
    TestKit.shutdownActorSystem(system)
  }
/*
  "The Publisher" must {

    "publish a single event" in {
      val p = MockPublisher.actor(system,ME1)

      val probe = MockSubscriber.probe(p)

      p ! MockPublisher.Publish

      probe.expectMsg(ME1)
      probe.reply(Publisher.StreamAck)

      probe.expectNoMessage
    }

    "publish 10 events" in {
      val es = Seq(ME1,ME1,ME1,ME1,ME1,ME1,ME1,ME1,ME1,ME1)
      val p = MockPublisher.actor(system,es:_*)

      val probe = MockSubscriber.probe(p)

      p ! MockPublisher.Publish

      es map { x =>
        probe.expectMsg(ME1)
        probe.reply(Publisher.StreamAck)
      }

      probe.expectNoMessage
    }

    "publish 1 event to twice subscriber" in {
      val p = MockPublisher.actor(system,ME1)

      val probe = MockSubscriber.probe(p)

      probe.send(p, Publisher.SubAndForget(None))
      probe.expectMsgType[Publisher.StreamInitAndForget]
      probe.reply(Publisher.StreamAck)

      p ! MockPublisher.Publish

      probe.expectMsg(ME1)
      probe.reply(Publisher.StreamAck)
      probe.expectMsg(ME1)
      probe.reply(Publisher.StreamAck)
      probe.expectNoMessage
    }

    "publish events to 2 probes" in {
      val p = MockPublisher.actor(system,ME1,MEX)

      val probe1 = MockSubscriber.probe(p)
      val probe2 = MockSubscriber.probe(p)

      p ! MockPublisher.Publish

      probe1.expectMsg(ME1)
      probe1.reply(Publisher.StreamAck)
      probe1.expectMsg(MEX)
      probe1.reply(Publisher.StreamAck)
      probe1.expectMsg(Publisher.StreamDone)
      probe1.expectNoMessage

      probe2.expectMsg(ME1)
      probe2.reply(Publisher.StreamAck)
      probe2.expectMsg(MEX)
      probe2.reply(Publisher.StreamAck)
      probe2.expectMsg(Publisher.StreamDone)
      probe2.expectNoMessage
    }
  }*/
}
 
class MockPublisher(events: Event*) extends Publisher {
  def receiveBehaviour: Receive = {
    case MockPublish => events map publish
  }
  override def receive = LoggingReceive { receiveBehaviour orElse publisherBehaviour }
}
case object MockPublish
