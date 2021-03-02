package sensordata

import cloudflow.akkastream._
import cloudflow.akkastream.scaladsl._
import cloudflow.streamlets.{ RoundRobinPartitioner, StreamletShape }
import cloudflow.streamlets.proto._

import com.lightbend.cinnamon.akka.stream.CinnamonAttributes._

class SensorDataToMetrics extends AkkaStreamlet {
  val in    = ProtoInlet[SensorData]("in")
  val out   = ProtoOutlet[Metric]("out").withPartitioner(RoundRobinPartitioner)
  val shape = StreamletShape(in, out)
  def flow =
    FlowWithCommittableContext[SensorData]
      .mapConcat { data ⇒
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
  override def createLogic = new RunnableGraphStreamletLogic() {
    def runnableGraph =
      sourceWithCommittableContext(in)
        .via(flow)
        .to(committableSink(out))
        .named("SensorDataToMetrics")
        .instrumented(name = "SensorDataToMetrics", traceable = true)

  }
}
