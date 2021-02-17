package client.sensordata

import akka.{Done, NotUsed}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.stream.scaladsl.Source
import scalapb.json4s.JsonFormat
import sensordata.{SensorData, SensorDataReply, SensorDataServiceClient}
import scala.collection.mutable.ListBuffer

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object SensorDataClientStream extends App {

    implicit val sys: ActorSystem[_] = ActorSystem(Behaviors.empty, "SensorDataClient")
    implicit val ec: ExecutionContext = sys.executionContext

    val client = SensorDataServiceClient(GrpcClientSettings.fromConfig("client.SensorDataService"))

    val filenames =
        if (args.isEmpty) {
            List("../test-data/one-record-per-line.json")
        }
        else {
            args.toList
        }

    var lines = ListBuffer.empty[String]
    for {filename <- filenames} {
        for (line <- scala.io.Source.fromFile(filename).getLines) {
            lines += line
        }
    }

    streamSensorData

    def streamSensorData : Unit = {
        var i = 0
        val requestStream: Source[SensorData, NotUsed] =
            Source.fromIterator(() => lines.iterator.iterator)
              .map(json => {
                  val j = JsonFormat.fromJsonString[SensorData](json)
                  i = i + 1
                  println(s"transmitting data for device Id ($i): ${j.deviceId}")
                  j
              })
              .mapMaterializedValue(_ => NotUsed)

        val responseStream: Source[SensorDataReply, NotUsed] = client.provideStreamed(requestStream)
        var cnt = 0
        val done: Future[Done] =
            responseStream.runForeach{ reply =>
                cnt = cnt + 1
                println(s"streaming reply received ($cnt): ${reply.deviceId}")
            }

        done.onComplete {
            case Success(_) =>
                println("streamingBroadcast done")
                System.exit(0)
            case Failure(e) =>
                println(s"Error streamingBroadcast: $e")
                System.exit(0)
        }
    }
}