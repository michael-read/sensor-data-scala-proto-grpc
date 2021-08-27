package sensordata

import akka.NotUsed
import akka.kafka.ConsumerMessage
import akka.stream.scaladsl.{ FlowWithContext, RunnableGraph }
import cloudflow.akkastream._
import cloudflow.akkastream.scaladsl._
import cloudflow.streamlets._
import cloudflow.streamlets.proto._
import com.lightbend.cinnamon.akka.stream.CinnamonAttributes

// tag::validMetric[]
class ValidMetricLogger extends AkkaStreamlet {

  val inlet: ProtoInlet[Metric] = ProtoInlet[Metric]("in")
  val shape: StreamletShape     = StreamletShape.withInlets(inlet)

  val LogLevel: RegExpConfigParameter = RegExpConfigParameter(
    "log-level",
    "Provide one of the following log levels, debug, info, warning or error",
    "^debug|info|warning|error$",
    Some("info")
  )

  val MsgPrefix: StringConfigParameter = StringConfigParameter("msg-prefix", "Provide a prefix for the log lines", Some("valid-logger"))

  override def configParameters = Vector(LogLevel, MsgPrefix)

  override def createLogic: RunnableGraphStreamletLogic = new RunnableGraphStreamletLogic() {
    val logF: String ⇒ Unit = LogLevel.value.toLowerCase match {
      case "debug"   ⇒ system.log.debug _
      case "info"    ⇒ system.log.info _
      case "warning" ⇒ system.log.warning _
      case "error"   ⇒ system.log.error _
    }

    val msgPrefix: String = MsgPrefix.value

    def log(metric: Metric): Unit =
      logF(s"$msgPrefix $metric")

    def flow: FlowWithContext[Metric, ConsumerMessage.Committable, Metric, ConsumerMessage.Committable, NotUsed] =
      FlowWithCommittableContext[Metric]
        .map { validMetric ⇒
          log(validMetric)
          validMetric
        }
        /*
              Note: if you don't currently have a Lightbend subscription you can optionally comment
              out the following line referencing CinnamonAttributes and associated import above.
         */
        .withAttributes(CinnamonAttributes.instrumented(name = "ValidMetricLogger"))

    def runnableGraph: RunnableGraph[_] =
      sourceWithCommittableContext(inlet)
        .via(flow)
        .to(committableSink)

  }

}
// end::validMetric[]
