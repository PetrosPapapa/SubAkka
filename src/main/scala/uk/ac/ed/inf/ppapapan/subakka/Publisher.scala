package uk.ac.ed.inf.ppapapan.subakka

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem }
import akka.event.LoggingReceive

trait Publisher[Event] extends Actor with ActorLogging {

  def isFinalEvent(e: Event): Boolean = false

  def doPublish(evt: Event): Unit

  def stopStream(): Unit

  def subAndForget(actor: ActorRef, ack: ActorRef): Unit

  def subscribe(name: String, actor: ActorRef, ack: ActorRef): SubscriptionSwitch


  def publish(evt: Event): Unit = {
    log.debug("Publishing Event: {}", evt.getClass().toString())
    doPublish(evt)
    if (isFinalEvent(evt)) stopStream()
  }

  def publisherBehaviour: Receive = {
    case Publisher.SubAndForget(ack) => subAndForget(sender,ack.getOrElse(sender()))
    case Publisher.Subscribe(name, ack) => subscribe(name,sender,ack.getOrElse(sender()))
    case Publisher.StreamAck => Unit
  }

  override def receive = LoggingReceive { publisherBehaviour }
}


object Publisher {
  case class SubAndForget(ack: Option[ActorRef])
  case class Subscribe(name: String, ack: Option[ActorRef])

  case object StreamAck
  case class StreamInitAndForget(ack: ActorRef)
  case class StreamInit(ack: ActorRef, killSwitch: SubscriptionSwitch)
  case object StreamDone
  case class StreamFail(ex: Throwable)
}

trait SubscriptionSwitch {
  def stop(): Unit
}
