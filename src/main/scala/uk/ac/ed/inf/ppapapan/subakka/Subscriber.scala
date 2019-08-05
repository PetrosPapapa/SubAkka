package uk.ac.ed.inf.ppapapan.subakka

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.stream.KillSwitch
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.duration._

trait Subscriber {
  def onInit(publisher: ActorRef): Unit = ()
  def onInit(publisher: ActorRef, k: KillSwitch): Unit = onInit(publisher)
  def onEvent(event: Event): Unit = ()
  def onDone(publisher: ActorRef, subscriber: ActorRef): Unit = ()
  def onFail(e: Throwable, publisher: ActorRef, subscriber: ActorRef): Unit = ()
}

class SubscriberActor(subscriber: Subscriber) extends Actor with ActorLogging {

  def subscriberBehaviour: Receive = {
    case Publisher.StreamInit(ack) => {
      log.debug("Stream initialized.")
      subscriber.onInit(sender())
      sender() ! Publisher.StreamAck
      ack.forward(Publisher.StreamInit(ack))
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

