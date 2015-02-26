package se.marcuslonnberg.scaladocker.remote.models.json

import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.json4s.{CustomSerializer, Extraction}
import se.marcuslonnberg.scaladocker.remote.models.JsonFormatHelpers._
import se.marcuslonnberg.scaladocker.remote.models._

object HostConfigSerializer extends CustomSerializer[HostConfig](implicit formats => ( {
  case obj: JObject =>
    implicit val o = obj

    val binds = extractFieldList[String]("Binds").map(JsonFormats.deserializeBinding)
    val lxcConf = extractFieldList[String]("LxcConf")
    val privileged = extractField[Boolean]("Privileged")
    val portBindings = JsonFormats.deserializePortBindings(extractField[JValue]("PortBindings"))
    val links = extractFieldList[ContainerLink]("Links")
    val publishAllPorts = extractField[Boolean]("PublishAllPorts")
    val dns = extractFieldList[String]("Dns")
    val dnsSearch = extractFieldList[String]("DnsSearch")
    val volumesFrom = extractFieldList[String]("VolumesFrom")
    val devices = extractFieldList[DeviceMapping]("Devices")
    val networkMode = extractField[String]("NetworkMode")
    val capAdd = extractFieldList[String]("CapAdd")
    val capDrop = extractFieldList[String]("CapDrop")
    val restartPolicy = extractFieldOpt[RestartPolicy]("RestartPolicy").getOrElse(NeverRestart)

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
