package uk.ac.ed.inf.ppapapan.subakka

import akka.NotUsed
import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem }
import akka.event.LoggingReceive
import akka.stream.{ ActorMaterializer, OverflowStrategy }
import akka.stream.scaladsl.{ BroadcastHub, Keep, Sink, Source, SourceQueueWithComplete }

sealed trait Event
case object EX extends Event
case object E1 extends Event
case object E2 extends Event
case object E3 extends Event
case object E4 extends Event
case object E5 extends Event

trait Publisher extends Actor with ActorLogging {
  implicit val materializer = ActorMaterializer()

  private val sourceQueue = Source.queue[Event](Publisher.bufferSize, Publisher.overflowStrategy)
  private val (
    queue: SourceQueueWithComplete[Event],
    source: Source[Event, NotUsed]
  ) = {
    val (q,s) = sourceQueue.toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both).run()
    s.runWith(Sink.ignore)
    (q,s)
  }

  def publish(evt: Event) = {
    log.debug("Publishing Event: {}", evt.getClass().toString())
    queue.offer(evt)
    evt match {
      case EX => queue.complete()
      case _ => Unit
    }
 }

  def subscribe(actor: ActorRef, ack: ActorRef): Unit =
    source.runWith(
      Sink.actorRefWithAck(
        actor,
        onInitMessage = Publisher.StreamInit(ack),
        ackMessage = Publisher.StreamAck,
        onCompleteMessage = Publisher.StreamDone,
        onFailureMessage = onErrorMessage))

  def onErrorMessage(ex: Throwable) = Publisher.StreamFail(ex)

  def publisherBehaviour: Receive = {
    case Publisher.Subscribe(sub, ack) => subscribe(sub, ack.getOrElse(sender()))
    case Publisher.StreamAck => Unit
  }

  override def receive = LoggingReceive { publisherBehaviour }
}

object Publisher {
  final val bufferSize = 5
  final val overflowStrategy = OverflowStrategy.backpressure

  case class Subscribe(sub: ActorRef, ack: Option[ActorRef])

  case object StreamAck
  case class StreamInit(ack: ActorRef)
  case object StreamDone
  case class StreamFail(ex: Throwable)
}
