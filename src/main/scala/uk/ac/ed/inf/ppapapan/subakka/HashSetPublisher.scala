package uk.ac.ed.inf.ppapapan.subakka

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem }
import akka.event.LoggingReceive
import scala.collection.mutable.HashSet

trait HashSetPublisher[Event] extends Publisher[Event] {
  val subscribers: HashSet[ActorRef] = HashSet[ActorRef]()

  override def doPublish(evt: Event): Unit = subscribers map (_ ! evt)

  override def subAndForget(actor: ActorRef, ack: ActorRef): Unit = {
    subscribers += actor
    actor ! Publisher.StreamInitAndForget(ack)
  }

  override def subscribe(name: String, actor: ActorRef, ack: ActorRef): SubscriptionSwitch = {
    subscribers += actor
    val switch = new SubscriptionHashSetSwitch(self, actor)
    actor ! Publisher.StreamInit(ack, switch)
    switch
  }

  override def stop(): Unit = {
    subscribers map (_ ! Publisher.StreamDone)
    subscribers.clear()
  }

  def unsubscribe(actor: ActorRef): Unit = subscribers -= actor

  def unsubBehaviour: Receive = {
    case HashSetPublisher.Unsubscribe(actor) => unsubscribe(actor)
  }

  override def receive = LoggingReceive { unsubBehaviour orElse publisherBehaviour }
}

object HashSetPublisher {
  case class Unsubscribe(actor: ActorRef)
}

class SubscriptionHashSetSwitch(publisher: ActorRef, subscriber: ActorRef) extends SubscriptionSwitch {
  override def stop(): Unit = {
    publisher ! HashSetPublisher.Unsubscribe(subscriber)
  }
}
