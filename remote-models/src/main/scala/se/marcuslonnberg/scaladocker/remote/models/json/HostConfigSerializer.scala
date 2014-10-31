package se.marcuslonnberg.scaladocker.remote.models.json

import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.json4s.{CustomSerializer, Extraction}
import se.marcuslonnberg.scaladocker.remote.models.JsonFormatHelpers._
import se.marcuslonnberg.scaladocker.remote.models._

object HostConfigSerializer extends CustomSerializer[HostConfig](implicit formats => ( {
  case obj: JObject =>
    implicit val o = obj

    val binds = extractField[List[String]]("Binds").map(JsonFormats.deserializeBinding)
    val lxcConf = extractField[List[String]]("LxcConf")
    val privileged = extractField[Boolean]("Privileged")
    val portBindings = JsonFormats.deserializePortBindings(extractField[JValue]("PortBindings"))
    val links = extractField[List[ContainerLink]]("Links")
    val publishAllPorts = extractField[Boolean]("PublishAllPorts")
    val dns = extractField[List[String]]("Dns")
    val dnsSearch = extractField[List[String]]("DnsSearch")
    val volumesFrom = extractField[List[String]]("VolumesFrom")
    val devices = extractField[List[DeviceMapping]]("Devices")
    val networkMode = extractField[String]("NetworkMode")
    val capAdd = extractField[List[String]]("CapAdd")
    val capDrop = extractField[List[String]]("CapDrop")
    val restartPolicy = extractFieldOpt[RestartPolicy]("RestartPolicy").getOrElse(RestartPolicy())

    HostConfig(
      binds = binds,
      lxcConf = lxcConf,
      privileged = privileged,
      portBindings = portBindings,
      links = links,
      publishAllPorts = publishAllPorts,
      dns = dns,
      dnsSearch = dnsSearch,
      volumesFrom = volumesFrom,
      devices = devices,
      networkMode = networkMode,
      capAdd = capAdd,
      capDrop = capDrop,
      restartPolicy = restartPolicy)
}, {
  case hc: HostConfig =>
    ("Binds" -> hc.binds.map(JsonFormats.serializeBinding)) ~
      ("LxcConf" -> hc.lxcConf) ~
      ("Privileged" -> hc.privileged) ~
      ("PortBindings" -> JsonFormats.serializePortBindings(hc.portBindings)) ~
      ("Links" -> Extraction.decompose(hc.links)) ~
      ("PublishAllPorts" -> hc.publishAllPorts) ~
      ("Dns" -> hc.dns) ~
      ("DnsSearch" -> hc.dnsSearch) ~
      ("VolumesFrom" -> hc.volumesFrom) ~
      ("Devices" -> Extraction.decompose(hc.devices)) ~
      ("NetworkMode" -> hc.networkMode) ~
      ("CapAdd" -> hc.capAdd) ~
      ("CapDrop" -> hc.capDrop) ~
      ("RestartPolicy" -> Extraction.decompose(hc.restartPolicy))
}))
