package uk.ac.ed.inf.ppapapan.subakka

import akka.NotUsed
import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem }
import akka.event.LoggingReceive
import akka.stream.{ ActorMaterializer, KillSwitch, KillSwitches, OverflowStrategy }
import akka.stream.scaladsl.{ BroadcastHub, Keep, Sink, Source, SourceQueueWithComplete }

trait Publisher[Event] extends Actor with ActorLogging {
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

  def isFinalEvent(e: Event): Boolean = false

  def publish(evt: Event) = {
    log.debug("Publishing Event: {}", evt)
    queue.offer(evt)
    if (isFinalEvent(evt)) queue.complete()
  }

  def subAndForget(actor: ActorRef, ack: ActorRef): Unit =
    source.runWith(
      Sink.actorRefWithAck(
        actor,
        onInitMessage = Publisher.StreamInitAndForget(ack),
        ackMessage = Publisher.StreamAck,
        onCompleteMessage = Publisher.StreamDone,
        onFailureMessage = onErrorMessage))

  def subscribe(name: String, actor: ActorRef, ack: ActorRef): KillSwitch = {
    // Using a shared kill switch even though we only want to shutdown one stream, because
    // the shared switch is available immediately (pre-materialization) so we can use it
    // in the sink.
    val killSwitch = KillSwitches.shared(name)
    source
      .via(killSwitch.flow)
      .runWith(
        Sink.actorRefWithAck(
          actor,
          onInitMessage = Publisher.StreamInit(ack,killSwitch),
          ackMessage = Publisher.StreamAck,
          onCompleteMessage = Publisher.StreamDone,
          onFailureMessage = onErrorMessage)
      )
    killSwitch
  }

  def onErrorMessage(ex: Throwable) = Publisher.StreamFail(ex)


  def publisherBehaviour: Receive = {
    case Publisher.SubAndForget(ack) => subAndForget(sender,ack.getOrElse(sender()))
    case Publisher.Subscribe(name, ack) => subscribe(name,sender,ack.getOrElse(sender()))
    case Publisher.StreamAck => Unit
  }

  override def receive = LoggingReceive { publisherBehaviour }
}


object Publisher {
  final val bufferSize = 5
  final val overflowStrategy = OverflowStrategy.backpressure

  case class SubAndForget(ack: Option[ActorRef])
  case class Subscribe(name: String, ack: Option[ActorRef])

  case object StreamAck
  case class StreamInitAndForget(ack: ActorRef)
  case class StreamInit(ack: ActorRef, killSwitch: KillSwitch)
  case object StreamDone
  case class StreamFail(ex: Throwable)
}
