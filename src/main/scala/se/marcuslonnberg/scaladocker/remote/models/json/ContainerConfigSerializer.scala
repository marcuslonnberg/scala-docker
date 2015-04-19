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
    val memory = extractField[Long]("Memory")
    val memorySwap = extractField[Long]("MemorySwap")
    val cpuShares = extractField[Long]("CpuShares")
    val cpuset = extractFieldOpt[String]("Cpuset")
    val attachStdin = extractField[Boolean]("AttachStdin")
    val attachStdout = extractField[Boolean]("AttachStdout")
    val attachStderr = extractField[Boolean]("AttachStderr")
    val exposedPorts = JsonFormats.deserializePortBindings(extractField[JValue]("ExposedPorts")).keys.toSeq
    val tty = extractField[Boolean]("Tty")
    val openStdin = extractField[Boolean]("OpenStdin")
    val stdinOnce = extractField[Boolean]("StdinOnce")
    val env = extractFieldList[String]("Env")
    val cmd = extractFieldList[String]("Cmd")
    val volumes = extractFieldList[String]("Volumes")
    val workingDir = extractFieldOpt[String]("WorkingDir")
    val entrypoint = extractFieldList[String]("Entrypoint")
    val networkDisabled = extractField[Boolean]("NetworkDisabled")
    val onBuild = extractFieldList[String]("OnBuild")

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

    def emptySeqToNull(xs: Seq[String]): JValue = {
      if (xs.isEmpty) JNull
      else JArray(xs.map(JString).toList)
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
      ("ExposedPorts" -> JsonFormats.serializePortBindings(cc.exposedPorts.map(_ -> Seq.empty).toMap)) ~
      ("Tty" -> cc.tty) ~
      ("OpenStdin" -> cc.openStdin) ~
      ("StdinOnce" -> cc.stdinOnce) ~
      ("Env" -> emptySeqToNull(cc.env)) ~
      ("Cmd" -> emptySeqToNull(cc.cmd)) ~
      ("Volumes" -> cc.volumes) ~
      ("WorkingDir" -> cc.workingDir) ~
      ("Entrypoint" -> emptySeqToNull(cc.entryPoint)) ~
      ("NetworkDisabled" -> cc.networkDisabled) ~
      ("OnBuild" -> cc.onBuild)
}))
