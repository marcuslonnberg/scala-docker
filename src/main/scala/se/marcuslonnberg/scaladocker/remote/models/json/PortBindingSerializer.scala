package se.marcuslonnberg.scaladocker.remote.models.json

import org.json4s.CustomSerializer
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import se.marcuslonnberg.scaladocker.remote.models.JsonFormatHelpers._
import se.marcuslonnberg.scaladocker.remote.models.PortBinding

object PortBindingSerializer extends CustomSerializer[PortBinding](implicit formats => ( {
  case obj: JObject =>
    implicit val o = obj

    val hostIp = extractField[String]("HostIp")
    val hostPort = extractField[String]("HostPort").toInt

    PortBinding(hostIp, hostPort)
}, {
  case pb: PortBinding =>
    ("HostIp" -> pb.hostIp) ~
      ("HostPort" -> pb.hostPort.toString)
}))
