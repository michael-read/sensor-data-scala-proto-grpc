package sensordata

import akka.NotUsed
import akka.kafka.ConsumerMessage
import akka.stream.scaladsl.{ FlowWithContext, RunnableGraph }

import java.util.UUID
import cloudflow.akkastream._
import cloudflow.akkastream.scaladsl._
import cloudflow.akkastream.util.scaladsl._
import cloudflow.streamlets._
import cloudflow.streamlets.proto._
import com.lightbend.cinnamon.akka.stream.CinnamonAttributes

// tag::validation[]
class MetricsValidation extends AkkaStreamlet {
  val in: ProtoInlet[Metric] = ProtoInlet[Metric]("in")
  val invalid: ProtoOutlet[InvalidMetric] = ProtoOutlet[InvalidMetric]("invalid")
    .withPartitioner(invalidMetric ⇒
      invalidMetric.metric match {
        case Some(metric) => metric.deviceId
        // this shouldn't happen, but just in case create a UUID
        case None => UUID.randomUUID().toString
      }
    )
  val valid: ProtoOutlet[Metric] = ProtoOutlet[Metric]("valid").withPartitioner(RoundRobinPartitioner)
  val shape: StreamletShape      = StreamletShape(in).withOutlets(invalid, valid)

  override def createLogic: AkkaStreamletLogic = new RunnableGraphStreamletLogic() {
    def runnableGraph: RunnableGraph[_] =
      sourceWithCommittableContext(in)
        .to(Splitter.sink(flow, invalid, valid))

    def flow: FlowWithContext[Metric, ConsumerMessage.Committable, Either[InvalidMetric, Metric], ConsumerMessage.Committable, NotUsed] =
      FlowWithCommittableContext[Metric]
        .map { metric ⇒
          if (!SensorDataUtils.isValidMetric(metric)) {
            if (system.log.isDebugEnabled) {
              system.log.debug(s"${metric.deviceId} ${metric.name} = ${metric.value} All metrics must be positive numbers")
            } else {
              system.log.info("debug is not enabled.")
            }
            Left(InvalidMetric(Some(metric), "All measurements must be positive numbers!"))
          } else Right(metric)
        }
        /*
              Note: if you don't currently have a Lightbend subscription you can optionally comment
              out the following line referencing CinnamonAttributes and associated import above.
         */
        .withAttributes(CinnamonAttributes.instrumented(name = "MetricsValidation"))
  }
}
// end::validation[]
