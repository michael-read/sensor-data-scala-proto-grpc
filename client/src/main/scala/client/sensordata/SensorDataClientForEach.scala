package client.sensordata

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Failure
import scala.util.Success
import akka.Done
import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.stream.scaladsl.Source
import scalapb.json4s.JsonFormat
import sensordata.{SensorData, SensorDataReply, SensorDataServiceClient}

import scala.collection._

object SensorDataClientForEach extends App {

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

    var cnt: Int = 0
    val replies = mutable.ListBuffer.empty[Future[SensorDataReply]]
    for {filename <- filenames} {
        for (line <- scala.io.Source.fromFile(filename).getLines) {
            cnt = cnt + 1
            replies += singleRequestReply(line, cnt)
//            singleRequestReply(line, cnt)
        }
    }
    println(s"requests sent ${replies.size}")
    Await.result(Future.sequence(replies), Duration(5, MINUTES))
    println("Done.")
    System.exit(0)

    def singleRequestReply(json: String, cnt: Int): Future[SensorDataReply] = {
        val data = JsonFormat.fromJsonString[SensorData](json)
        println(s"transmitting data for device Id ($cnt): ${data.deviceId}")
        val reply = client.provide(data)
        reply.onComplete {
            case Success(msg) =>
                println(s"received response for device Id ($cnt): ${msg.deviceId}")
            case Failure(e) =>
                println(s"Error ($cnt): $e")
        }
//        Await.result(reply, Duration(5, SECONDS))
        reply
    }

}