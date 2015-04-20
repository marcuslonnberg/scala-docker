package se.marcuslonnberg.scaladocker.remote.models.json

import com.github.nscala_time.time.Imports._
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import org.json4s._
import se.marcuslonnberg.scaladocker.remote.models.JsonFormatHelpers._
import se.marcuslonnberg.scaladocker.remote.models._

object ContainerInfoSerializer extends CustomSerializer[ContainerInfo](implicit formats => ( {
  case obj: JObject =>
    implicit val o = obj

    val id = extractFieldOpt[ContainerHashId]("Id")
      .getOrElse(extractField[ContainerHashId]("ID"))
    val created = extractField[DateTime]("Created")
    val path = extractField[String]("Path")
    val args = extractField[Seq[String]]("Args")
    val config = extractField[ContainerConfig]("Config")
    val state = extractField[ContainerState]("State")
    val image = extractField[String]("Image")
    val networkSettings = extractField[NetworkSettings]("NetworkSettings")
    val resolvConfPath = extractField[String]("ResolvConfPath")
    val hostnamePath = extractField[String]("HostnamePath")
    val hostsPath = extractField[String]("HostsPath")
    val name = extractField[String]("Name")
    val driver = extractField[String]("Driver")
    val execDriver = extractField[String]("ExecDriver")
    val mountLabel = extractFieldOpt[String]("MountLabel")
    val processLabel = extractFieldOpt[String]("ProcessLabel")
    val volumes = JsonFormats.deserializeVolumes(extractField[JObject]("Volumes"), extractField[JObject]("VolumesRW"))
    val hostConfig = extractField[HostConfig]("HostConfig")

    ContainerInfo(
      id = id,
      created = created,
      path = path,
      args = args,
      config = config,
      state = state,
      image = image,
      networkSettings = networkSettings,
      resolvConfPath = resolvConfPath,
      hostnamePath = hostnamePath,
      hostsPath = hostsPath,
      name = name,
      driver = driver,
      execDriver = execDriver,
      mountLabel = mountLabel,
      processLabel = processLabel,
      volumes = volumes,
      hostConfig = hostConfig)
}, {
  case ci: ContainerInfo =>
    val (volumes, volumesRw) = JsonFormats.serializeVolumes(ci.volumes)
    ("Id" -> Extraction.decompose(ci.id)) ~
      ("Created" -> Extraction.decompose(ci.created)) ~
      ("Path" -> ci.path) ~
      ("Args" -> ci.args) ~
      ("Config" -> Extraction.decompose(ci.config)) ~
      ("State" -> Extraction.decompose(ci.state)) ~
      ("Image" -> ci.image) ~
      ("NetworkSettings" -> Extraction.decompose(ci.networkSettings)) ~
      ("ResolvConfPath" -> ci.resolvConfPath) ~
      ("HostnamePath" -> ci.hostnamePath) ~
      ("HostsPath" -> ci.hostsPath) ~
      ("Name" -> ci.name) ~
      ("Driver" -> ci.driver) ~
      ("ExecDriver" -> ci.execDriver) ~
      ("MountLabel" -> ci.mountLabel) ~
      ("ProcessLabel" -> ci.processLabel) ~
      ("Volumes" -> volumes) ~
      ("VolumesRW" -> volumesRw) ~
      ("HostConfig" -> Extraction.decompose(ci.hostConfig))
}))
