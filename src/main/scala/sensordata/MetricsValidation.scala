package sensordata

import java.util.UUID

import cloudflow.akkastream._
import cloudflow.akkastream.scaladsl._
import cloudflow.akkastream.util.scaladsl._
import cloudflow.streamlets._
import cloudflow.streamlets.proto._
import com.lightbend.cinnamon.akka.stream.CinnamonAttributes._

class MetricsValidation extends AkkaStreamlet {
  val in = ProtoInlet[Metric]("in")
  val invalid = ProtoOutlet[InvalidMetric]("invalid")
    .withPartitioner(invalidMetric ⇒
      invalidMetric.metric match {
        case Some(metric) => metric.deviceId
        // this shouldn't happen, but just in case create a UUID
        case None => UUID.randomUUID().toString
      }
    )
  val valid = ProtoOutlet[Metric]("valid").withPartitioner(RoundRobinPartitioner)
  val shape = StreamletShape(in).withOutlets(invalid, valid)

  override def createLogic = new RunnableGraphStreamletLogic() {
    def runnableGraph =
      sourceWithCommittableContext(in)
        .to(Splitter.sink(flow, invalid, valid))
        .named("MetricsValidation")
        .instrumented(name = "MetricsValidation", traceable = true)

    def flow =
      FlowWithCommittableContext[Metric]
        .map { metric ⇒
          if (!SensorDataUtils.isValidMetric(metric)) Left(InvalidMetric(Some(metric), "All measurements must be positive numbers!"))
          else Right(metric)
        }
  }
}
