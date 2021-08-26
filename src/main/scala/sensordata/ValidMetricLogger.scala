package sensordata

import cloudflow.akkastream._
import cloudflow.akkastream.scaladsl._
import cloudflow.streamlets._
import cloudflow.streamlets.proto._
import com.lightbend.cinnamon.akka.stream.CinnamonAttributes

// tag::validMetric[]
class ValidMetricLogger extends AkkaStreamlet {

  val inlet = ProtoInlet[Metric]("in")
  val shape = StreamletShape.withInlets(inlet)

  val LogLevel = RegExpConfigParameter(
    "log-level",
    "Provide one of the following log levels, debug, info, warning or error",
    "^debug|info|warning|error$",
    Some("info")
  )

  val MsgPrefix = StringConfigParameter("msg-prefix", "Provide a prefix for the log lines", Some("valid-logger"))

  override def configParameters = Vector(LogLevel, MsgPrefix)

  override def createLogic = new RunnableGraphStreamletLogic() {
    val logF: String ⇒ Unit = LogLevel.value.toLowerCase match {
      case "debug"   ⇒ system.log.debug _
      case "info"    ⇒ system.log.info _
      case "warning" ⇒ system.log.warning _
      case "error"   ⇒ system.log.error _
    }

    val msgPrefix = MsgPrefix.value

    def log(metric: Metric) =
      logF(s"$msgPrefix $metric")

    def flow =
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

    def runnableGraph =
      sourceWithCommittableContext(inlet)
        .via(flow)
        .to(committableSink)

  }

}
// end::validMetric[]