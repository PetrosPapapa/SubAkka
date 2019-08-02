package uk.ac.ed.inf.ppapapan.subakka

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.stream.KillSwitch
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag

trait Subscriber[Event] {
  def onInit(publisher: ActorRef): Unit = ()
  def onInit(publisher: ActorRef, k: KillSwitch): Unit = onInit(publisher)
  def onEvent(event: Event): Unit = ()
  def onDone(publisher: ActorRef, subscriber: ActorRef): Unit = ()
  def onFail(e: Throwable, publisher: ActorRef, subscriber: ActorRef): Unit = ()

  def subAndForgetTo(
    publisher: ActorRef,
    name: Option[String] = None,
    timeout: FiniteDuration = 3.seconds)
    (implicit system: ActorSystem, tag: ClassTag[Event]): Future[Any] = {
    implicit val tOut = Timeout(timeout)
    Subscriber.actor(this,name) ? Subscriber.SubAndForgetTo(publisher)
  }

  def subscribeTo(
    publisher: ActorRef,
    name: Option[String] = None,
    timeout: FiniteDuration = 3.seconds)
    (implicit system: ActorSystem, tag: ClassTag[Event]): Future[Publisher.StreamInit] = {
    implicit val tOut = Timeout(timeout)
    val switchName = name.getOrElse("SubscriberKillSwitch")
    (Subscriber.actor(this,name) ? Subscriber.SubscribeTo(switchName,publisher)).mapTo[Publisher.StreamInit]
  }
}

class SubscriberActor[Event](subscriber: Subscriber[Event])(implicit tag: ClassTag[Event]) extends Actor with ActorLogging {

  def subscriberBehaviour: Receive = {
    case Subscriber.SubAndForgetTo(publisher) => {
      log.debug(s"SubAndFogetting to: $publisher")
      publisher ! Publisher.SubAndForget(Some(sender))
    }
    case Subscriber.SubscribeTo(name,publisher) => {
      log.debug(s"Subscribing to: $publisher")
      publisher ! Publisher.Subscribe(name,Some(sender))
    }
    case Publisher.StreamInitAndForget(ack) => {
      log.debug("Stream initialized.")
      subscriber.onInit(sender())
      sender() ! Publisher.StreamAck
      ack.forward(Publisher.StreamInitAndForget(ack))
    }
    case Publisher.StreamInit(ack,killSwitch) => {
      log.debug("Stream initialized with kill switch.")
      subscriber.onInit(sender(),killSwitch)
      sender() ! Publisher.StreamAck
      ack.forward(Publisher.StreamInit(ack,killSwitch))
    }
    case Publisher.StreamDone => {
      log.debug("Stream completed.")
      subscriber.onDone(sender(),self)
    }
    case Publisher.StreamFail(ex) => {
      log.error(ex, "Stream failed!")
      subscriber.onFail(ex,sender(),self)
    }
    case e: Event => {
      log.debug("Observing Event: {}",e)
      subscriber.onEvent(e)
      sender() ! Publisher.StreamAck
    }
  }

  override def receive = LoggingReceive { subscriberBehaviour }
}

object Subscriber {
  case class SubAndForgetTo(publisher: ActorRef)
  case class SubscribeTo(name: String, publisher: ActorRef)

  def props[Event](subscriber: Subscriber[Event])(implicit tag: ClassTag[Event]): Props = { Props(new SubscriberActor(subscriber)) }
  def actor[Event](subscriber: Subscriber[Event], name: Option[String] = None)(implicit system: ActorSystem, tag: ClassTag[Event]): ActorRef = name match {
    case None => system.actorOf(props(subscriber))
    case Some(n) => system.actorOf(props(subscriber),n)
 }
}

