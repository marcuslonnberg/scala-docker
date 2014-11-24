package se.marcuslonnberg.scaladocker.remote.models.json

import org.joda.time.DateTime
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import org.json4s._
import se.marcuslonnberg.scaladocker.remote.models.JsonFormatHelpers._
import se.marcuslonnberg.scaladocker.remote.models._

object ContainerStateSerializer extends CustomSerializer[ContainerState]({ implicit formats =>
  def extractDateTimeField(fieldName: String)(implicit obj: JObject): Option[DateTime] = {
    obj \ fieldName match {
      case JString("0001-01-01T00:00:00Z") => None
      case _ =>
        extractFieldOpt[DateTime](fieldName)
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
