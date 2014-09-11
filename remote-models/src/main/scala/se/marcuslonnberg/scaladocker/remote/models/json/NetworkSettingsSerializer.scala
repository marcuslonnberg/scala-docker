package se.marcuslonnberg.scaladocker.remote.models.json

import org.json4s.CustomSerializer
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import se.marcuslonnberg.scaladocker.remote.models.JsonFormatHelpers._
import se.marcuslonnberg.scaladocker.remote.models.{JsonFormats, NetworkSettings}

object NetworkSettingsSerializer extends CustomSerializer[NetworkSettings](implicit formats => ( {
  case obj: JObject =>
    implicit val o = obj

    val ipAddress = extractField[String]("IPAddress")
    val ipPrefixLen = extractField[Int]("IPPrefixLen")
    val gateway = extractField[String]("Gateway")
    val bridge = extractField[String]("Bridge")
    val ports = JsonFormats.deserializePortBindings(extractField[JObject]("Ports"))

    NetworkSettings(
      ipAddress = ipAddress,
      ipPrefixLength = ipPrefixLen,
      gateway = gateway,
      bridge = bridge,
      ports = ports)
}, {
  case ns: NetworkSettings =>
    ("IPAddress" -> ns.ipAddress) ~
      ("IPPrefixLen" -> ns.ipPrefixLength) ~
      ("Gateway" -> ns.gateway) ~
      ("Bridge" -> ns.bridge) ~
      ("Ports" -> JsonFormats.serializePortBindings(ns.ports))
}))
