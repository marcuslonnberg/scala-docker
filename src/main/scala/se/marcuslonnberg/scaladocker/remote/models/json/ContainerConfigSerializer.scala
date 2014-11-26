package se.marcuslonnberg.scaladocker.remote.models.json

import org.json4s.{Extraction, CustomSerializer}
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import se.marcuslonnberg.scaladocker.remote.models.JsonFormatHelpers._
import se.marcuslonnberg.scaladocker.remote.models.{ContainerConfig, ImageName, JsonFormats}

object ContainerConfigSerializer extends CustomSerializer[ContainerConfig](implicit formats => ( {
  case obj: JObject =>
    implicit val o = obj

    val image = extractField[ImageName]("Image")
    val hostname = extractFieldOpt[String]("Hostname")
    val domainName = extractFieldOpt[String]("Domainname")
    val user = extractFieldOpt[String]("User")
    val memory = extractFieldOpt[Long]("Memory")
    val memorySwap = extractFieldOpt[Long]("MemorySwap")
    val cpuShares = extractFieldOpt[Long]("CpuShares")
    val cpuset = extractFieldOpt[String]("Cpuset")
    val attachStdin = extractFieldOpt[Boolean]("AttachStdin")
    val attachStdout = extractFieldOpt[Boolean]("AttachStdout")
    val attachStderr = extractFieldOpt[Boolean]("AttachStderr")
    val exposedPorts = JsonFormats.deserializePortBindings(extractField[JValue]("ExposedPorts"))
    val tty = extractFieldOpt[Boolean]("Tty")
    val openStdin = extractFieldOpt[Boolean]("OpenStdin")
    val stdinOnce = extractFieldOpt[Boolean]("StdinOnce")
    val env = extractField[List[String]]("Env")
    val cmd = extractField[List[String]]("Cmd")
    val volumes = extractField[List[String]]("Volumes")
    val workingDir = extractFieldOpt[String]("WorkingDir")
    val entrypoint = extractField[List[String]]("Entrypoint")
    val networkDisabled = extractFieldOpt[Boolean]("NetworkDisabled")
    val onBuild = extractField[List[String]]("OnBuild")

    ContainerConfig(image = image,
      hostname = hostname,
      domainName = domainName,
      user = user,
      memory = memory,
      memorySwap = memorySwap,
      cpuShares = cpuShares,
      cpuset = cpuset,
      attachStdin = attachStdin,
      attachStdout = attachStdout,
      attachStderr = attachStderr,
      exposedPorts = exposedPorts,
      tty = tty,
      openStdin = openStdin,
      stdinOnce = stdinOnce,
      env = env,
      cmd = cmd,
      volumes = volumes,
      workingDir = workingDir,
      entryPoint = entrypoint,
      networkDisabled = networkDisabled,
      onBuild = onBuild)
}, {
  case cc: ContainerConfig =>
    val image = Extraction.decompose(cc.image)

    def emptyListToNull(xs: List[String]): JValue = {
      if (xs.isEmpty) JNull
      else JArray(xs.map(JString))
    }

    ("Image" -> image) ~
      ("Hostname" -> cc.hostname) ~
      ("Domainname" -> cc.domainName) ~
      ("User" -> cc.user) ~
      ("Memory" -> cc.memory) ~
      ("MemorySwap" -> cc.memorySwap) ~
      ("CpuShares" -> cc.cpuShares) ~
      ("Cpuset" -> cc.cpuset) ~
      ("AttachStdin" -> cc.attachStdin) ~
      ("AttachStdout" -> cc.attachStdout) ~
      ("AttachStderr" -> cc.attachStderr) ~
      ("ExposedPorts" -> JsonFormats.serializePortBindings(cc.exposedPorts)) ~
      ("Tty" -> cc.tty) ~
      ("OpenStdin" -> cc.openStdin) ~
      ("StdinOnce" -> cc.stdinOnce) ~
      ("Env" -> emptyListToNull(cc.env)) ~
      ("Cmd" -> emptyListToNull(cc.cmd)) ~
      ("Volumes" -> cc.volumes) ~
      ("WorkingDir" -> cc.workingDir) ~
      ("Entrypoint" -> emptyListToNull(cc.entryPoint)) ~
      ("NetworkDisabled" -> cc.networkDisabled) ~
      ("OnBuild" -> cc.onBuild)
}))
