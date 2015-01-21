package se.marcuslonnberg.scaladocker.remote.models.json

import org.json4s.CustomSerializer
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import se.marcuslonnberg.scaladocker.remote.models._
import se.marcuslonnberg.scaladocker.remote.models.JsonFormatHelpers._

object RestartPolicySerializer extends CustomSerializer[RestartPolicy](implicit formats => ( {
  case obj: JObject =>
    implicit val impObj = obj

    extractField[String]("Name") match {
      case RestartOnFailure.name =>
        RestartOnFailure(maximumRetryCount = extractField[Int]("MaximumRetryCount"))
      case AlwaysRestart.name =>
        AlwaysRestart
      case NeverRestart.name =>
        NeverRestart
    }
}, {
  case rof: RestartOnFailure =>
    ("Name" -> rof.name) ~ ("MaximumRetryCount" -> rof.maximumRetryCount)
  case rp: RestartPolicy =>
    ("Name" -> rp.name)
}))


