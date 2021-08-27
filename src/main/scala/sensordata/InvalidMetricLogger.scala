package sensordata

import akka.NotUsed
import akka.kafka.ConsumerMessage
import akka.stream.scaladsl.{ FlowWithContext, RunnableGraph }
import cloudflow.akkastream._
import cloudflow.akkastream.scaladsl._
import cloudflow.streamlets._
import cloudflow.streamlets.proto._
import com.lightbend.cinnamon.akka.stream.CinnamonAttributes

// tag::invalidMetric[]
class InvalidMetricLogger extends AkkaStreamlet {
  val inlet: ProtoInlet[InvalidMetric] = ProtoInlet[InvalidMetric]("in")
  val shape: StreamletShape            = StreamletShape.withInlets(inlet)

  override def createLogic: RunnableGraphStreamletLogic = new RunnableGraphStreamletLogic() {
    val flow: FlowWithContext[InvalidMetric, ConsumerMessage.Committable, InvalidMetric, ConsumerMessage.Committable, NotUsed] =
      FlowWithCommittableContext[InvalidMetric]
        .map { invalidMetric ⇒
          system.log.warning(s"Invalid metric detected!!! $invalidMetric")
          invalidMetric
        }
        /*
            Note: if you don't currently have a Lightbend subscription you can optionally comment
            out the following line referencing CinnamonAttributes and associated import above.
         */
        .withAttributes(CinnamonAttributes.instrumented(name = "InvalidMetricLogger"))

    def runnableGraph: RunnableGraph[_] =
      sourceWithCommittableContext(inlet)
        .via(flow)
        .to(committableSink)
  }

}
// end::invalidMetric[]
