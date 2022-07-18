package sensordata

import akka.grpc.scaladsl.ServerReflection
import cloudflow.akkastream._
import cloudflow.akkastream.util.scaladsl._
import cloudflow.streamlets._
import cloudflow.streamlets.proto._

// tag::ingress[]
class SensorDataGrpcIngress extends AkkaServerStreamlet {
  val out: ProtoOutlet[SensorData] = ProtoOutlet[SensorData]("out").withPartitioner(RoundRobinPartitioner)
  def shape(): StreamletShape      = StreamletShape.withOutlets(out)

  override def createLogic(): GrpcServerLogic = new GrpcServerLogic(this) {
    override def handlers() =
      List(SensorDataServiceHandler.partial(new SensorDataIngressImpl(sinkRef(out))), ServerReflection.partial(List(SensorDataService)))
  }
}
// end::ingress[]
