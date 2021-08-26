package sensordata

import akka.NotUsed
import akka.stream.scaladsl.Source
import cloudflow.akkastream.WritableSinkRef

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// tag::ingressImpl[]
class SensorDataIngressImpl(outlet: WritableSinkRef[SensorData]) extends SensorDataService {

  /**
   * Sends SensorData to the Ingress
   */
  override def provide(in: SensorData): Future[SensorDataReply] =
    outlet
      .write(in)
      .map { result =>
        SensorDataReply(deviceId = result.deviceId, success = true)
      }
      .recover {
        case ex: Exception =>
          SensorDataReply(deviceId = in.deviceId, success = false, ex.getMessage)
      }

  /**
   * Sends SensorData to the Ingress as a Stream
   */
  override def provideStreamed(in: Source[SensorData, NotUsed]): Source[SensorDataReply, NotUsed] =
    in.mapAsync(5) { sensordata =>
      outlet
        .write(sensordata)
        .map { result =>
          SensorDataReply(deviceId = result.deviceId, success = true)
        }
        .recover {
          case ex: Exception =>
            SensorDataReply(deviceId = sensordata.deviceId, success = false, ex.getMessage)
        }
    }
}
// end::ingressImpl[]