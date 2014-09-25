package se.marcuslonnberg.scaladocker.remote.models.json

import org.joda.time.format.ISODateTimeFormat
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import org.json4s._
import se.marcuslonnberg.scaladocker.remote.models.JsonFormatHelpers._
import se.marcuslonnberg.scaladocker.remote.models._

object ContainerStateSerializer extends CustomSerializer[ContainerState]({ implicit formats =>
  val DateExtractor = """(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d{3})\d+Z""".r
  val DateFormat = ISODateTimeFormat.dateTime
  def extractDateTimeField(fieldName: String)(implicit obj: JObject) = {
    extractFieldOpt[String](fieldName).filter(_ != "0001-01-01T00:00:00Z").map {
      case DateExtractor(isoFormat) =>
        DateFormat.parseDateTime(isoFormat + "Z")
    }
  }

  ({
    case obj: JObject =>
      implicit val o = obj

      val running = extractField[Boolean]("Running")
      val paused = extractField[Boolean]("Paused")
      val restarting = extractField[Boolean]("Restarting")
      val pid = extractField[Int]("Pid")
      val exitCode = extractField[Int]("ExitCode")

      val startedAt = extractDateTimeField("StartedAt")
      val finishedAt = extractDateTimeField("FinishedAt")

      ContainerState(
        running = running,
        paused = paused,
        restarting = restarting,
        pid = pid,
        exitCode = exitCode,
        startedAt = startedAt,
        finishedAt = finishedAt)
  }, {
    case state: ContainerState =>
      ("Running" -> state.running) ~
        ("Paused" -> state.paused) ~
        ("Restarting" -> state.restarting) ~
        ("Pid" -> state.pid) ~
        ("ExitCode" -> state.exitCode) ~
        ("StartedAt" -> Extraction.decompose(state.startedAt)) ~
        ("FinishedAt" -> Extraction.decompose(state.finishedAt))
  })
})
