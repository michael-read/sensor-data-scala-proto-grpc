package sensordata

import akka.NotUsed
import akka.kafka.ConsumerMessage
import akka.stream.scaladsl.{ FlowWithContext, RunnableGraph }
import cloudflow.akkastream._
import cloudflow.akkastream.scaladsl._
import cloudflow.streamlets.{ RoundRobinPartitioner, StreamletShape }
import cloudflow.streamlets.proto._
import com.lightbend.cinnamon.akka.stream.CinnamonAttributes

// tag::toMetrics[]
class SensorDataToMetrics extends AkkaStreamlet {
  val in: ProtoInlet[SensorData] = ProtoInlet[SensorData]("in")
  val out: ProtoOutlet[Metric]   = ProtoOutlet[Metric]("out").withPartitioner(RoundRobinPartitioner)
  val shape: StreamletShape      = StreamletShape(in, out)
  def flow(): FlowWithContext[SensorData, ConsumerMessage.Committable, Metric, ConsumerMessage.Committable, NotUsed] =
    FlowWithCommittableContext[SensorData]()
      .mapConcat { data =>
        data.measurements match {
          case Some(measurements) =>
            List(
              Metric(data.deviceId, data.timestamp, "power", measurements.power),
              Metric(data.deviceId, data.timestamp, "rotorSpeed", measurements.rotorSpeed),
              Metric(data.deviceId, data.timestamp, "windSpeed", measurements.windSpeed)
            )
          case None =>
            List()
        }
      }
      /*
      Note: if you don't currently have a Lightbend subscription you can optionally comment
      out the following line referencing CinnamonAttributes and associated import above.
       */
      .withAttributes(CinnamonAttributes.instrumented(name = "SensorDataToMetrics"))

  override def createLogic(): RunnableGraphStreamletLogic = new RunnableGraphStreamletLogic() {
    def runnableGraph(): RunnableGraph[_] =
      sourceWithCommittableContext(in)
        .via(flow())
        .to(committableSink(out))

  }
}
// end::toMetrics[]
