package uk.ac.ed.inf.ppapapan.subakka

import akka.NotUsed
import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem }
import akka.event.LoggingReceive
import akka.stream.{ ActorMaterializer, KillSwitch, KillSwitches, OverflowStrategy }
import akka.stream.scaladsl.{ BroadcastHub, Keep, Sink, Source, SourceQueueWithComplete }

trait StreamPublisher[Event] extends Publisher[Event] {
  implicit val materializer = ActorMaterializer()

  private val sourceQueue = Source.queue[Event](StreamPublisher.bufferSize, StreamPublisher.overflowStrategy)
  private val (
    queue: SourceQueueWithComplete[Event],
    source: Source[Event, NotUsed]
  ) = {
    val (q,s) = sourceQueue.toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both).run()
    s.runWith(Sink.ignore)
    (q,s)
  }

  override def doPublish(evt: Event) = queue.offer(evt)
  override def stopStream() = queue.complete()

  override def subAndForget(actor: ActorRef, ack: ActorRef): Unit =
    source.runWith(
      Sink.actorRefWithAck(
        actor,
        onInitMessage = Publisher.StreamInitAndForget(ack),
        ackMessage = Publisher.StreamAck,
        onCompleteMessage = Publisher.StreamDone,
        onFailureMessage = onErrorMessage))

  override def subscribe(name: String, actor: ActorRef, ack: ActorRef): SubscriptionSwitch = {
    // Using a shared kill switch even though we only want to shutdown one stream, because
    // the shared switch is available immediately (pre-materialization) so we can use it
    // in the sink.
    val killSwitch = KillSwitches.shared(name)
    val subSwitch = new SubscriptionStreamSwitch(killSwitch)
    source
      .via(killSwitch.flow)
      .runWith(
        Sink.actorRefWithAck(
          actor,
          onInitMessage = Publisher.StreamInit(ack,subSwitch),
          ackMessage = Publisher.StreamAck,
          onCompleteMessage = Publisher.StreamDone,
          onFailureMessage = onErrorMessage)
      )
    subSwitch
  }

  def onErrorMessage(ex: Throwable) = Publisher.StreamFail(ex)
}

object StreamPublisher {
  final val bufferSize = 5
  final val overflowStrategy = OverflowStrategy.backpressure
}

class SubscriptionStreamSwitch(switch: KillSwitch) extends SubscriptionSwitch {
  override def stop(): Unit = switch.shutdown()
}
