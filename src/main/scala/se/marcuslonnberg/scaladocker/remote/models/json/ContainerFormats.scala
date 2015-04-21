package se.marcuslonnberg.scaladocker.remote.models.json

import play.api.libs.functional.syntax._
import play.api.libs.json._
import se.marcuslonnberg.scaladocker.remote.models._

trait ContainerFormats extends CommonFormats {
  implicit val containerStatusFormat = {
    implicit val a = portBindingsArrayFormat
    implicit val b = dateTimeSecondsFormat
    JsonUtils.upperCamelCase(Json.format[ContainerStatus])
  }

  implicit val volumeBindingFormat = Format[Volume](Reads { in =>
    in.validate[String].flatMap { binding =>
      binding.split(":") match {
        case Array(host, container) =>
          JsSuccess(Volume(host, container, rw = true))
        case Array(host, container, readOnly) =>
          JsSuccess(Volume(host, container, readOnly != "ro"))
        case _ =>
          JsError(s"Could not parse binding: $binding")
      }
    }
  }, Writes { volume =>
    val maybeRw = if (volume.rw) None else Some("ro")
    JsString((volume.hostPath :: volume.containerPath :: maybeRw.toList).mkString(":"))
  })

  implicit val containerConfigFormat = Format[ContainerConfig](
    Reads { in =>
      ((JsPath \ "Image").read[ImageName] and
        (JsPath \ "Hostname").readNullable[String] and
        (JsPath \ "DomainName").readNullable[String] and
        (JsPath \ "User").readNullable[String] and
        (JsPath \ "Memory").read[Long] and
        (JsPath \ "MemorySwap").read[Long] and
        (JsPath \ "CpuShares").read[Long] and
        (JsPath \ "Cpuset").readNullable[String] and
        (JsPath \ "AttachStdin").read[Boolean] and
        (JsPath \ "AttachStdout").read[Boolean] and
        (JsPath \ "AttachStderr").read[Boolean] and
        (JsPath \ "ExposedPorts").readNullable[Seq[Port]].map(_.getOrElse(Seq.empty)) and
        (JsPath \ "Env").readNullable[Seq[String]].map(_.getOrElse(Seq.empty)) and
        (JsPath \ "Cmd").readNullable[Seq[String]].map(_.getOrElse(Seq.empty)) and
        (JsPath \ "Volumes").readNullable[Seq[String]].map(_.getOrElse(Seq.empty)) and
        (JsPath \ "WorkingDir").readNullable[String] and
        (JsPath \ "Entrypoint").readNullable[Seq[String]].map(_.getOrElse(Seq.empty)) and
        (JsPath \ "NetworkDisabled").read[Boolean] and
        (JsPath \ "Tty").read[Boolean] and
        (JsPath \ "OpenStdin").read[Boolean] and
        (JsPath \ "OnBuild").readNullable[Seq[String]].map(_.getOrElse(Seq.empty))
        ) { (image: ImageName, hostname: Option[String], domainName: Option[String], user: Option[String], memory: Long,
      memorySwap: Long, cpuShares: Long, cpuset: Option[String], attachStdin: Boolean, attachStdout: Boolean,
      attachStderr: Boolean, exposedPorts: Seq[Port], env: Seq[String], cmd: Seq[String], volumes: Seq[String],
      workingDir: Option[String], entryPoint: Seq[String], networkDisabled: Boolean, tty: Boolean, openStdin: Boolean,
      onBuild: Seq[String]) =>

        val stdinOnce = (in \ "stdinOnce").asOpt[Boolean].getOrElse(false)
        ContainerConfig(
          image = image,
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
          entryPoint = entryPoint,
          networkDisabled = networkDisabled,
          onBuild = onBuild
        )
      }.reads(in)
    }, Writes[ContainerConfig] { cc =>
      Json.obj(
        "Image" -> cc.image,
        "Hostname" -> cc.hostname,
        "DomainName" -> cc.domainName,
        "User" -> cc.user,
        "Memory" -> cc.memory,
        "MemorySwap" -> cc.memorySwap,
        "CpuShares" -> cc.cpuShares,
        "Cpuset" -> cc.cpuset,
        "AttachStdin" -> cc.attachStdin,
        "AttachStdout" -> cc.attachStdout,
        "AttachStderr" -> cc.attachStderr,
        "ExposedPorts" -> cc.exposedPorts,
        "Tty" -> cc.tty,
        "OpenStdin" -> cc.openStdin,
        "StdinOnce" -> cc.stdinOnce,
        "Env" -> cc.env,
        "Cmd" -> cc.cmd,
        "Volumes" -> cc.volumes,
        "WorkingDir" -> cc.workingDir,
        "Entrypoint" -> cc.entryPoint,
        "NetworkDisabled" -> cc.networkDisabled,
        "OnBuild" -> cc.onBuild
      )
    })

  implicit val restartPolicyFormat = Format[RestartPolicy](Reads { in =>
    (JsPath \ "Name").read[String].reads(in).flatMap {
      case RestartOnFailure.name =>
        (JsPath \ "MaximumRetryCount").read[Int].map(count => RestartOnFailure(count)).reads(in)
      case AlwaysRestart.name =>
        JsSuccess(AlwaysRestart)
      case NeverRestart.name =>
        JsSuccess(NeverRestart)
    }
  }, Writes {
    case rof: RestartOnFailure =>
      Json.obj(
        "Name" -> rof.name,
        "MaximumRetryCount" -> rof.maximumRetryCount
      )
    case rp: RestartPolicy =>
      Json.obj("Name" -> rp.name)
  })

  implicit val containerLinkFormat = Format[ContainerLink](
    JsPath.read[String].map {
      case ContainerLink(link) => link
    },
    Writes { containerLink =>
      JsString(containerLink.mkString)
    }
  )

  implicit val deviceMappingFormat = JsonUtils.upperCamelCase(Json.format[DeviceMapping])

  implicit val hostConfigFormat: Format[HostConfig] = ((JsPath \ "Binds").formatNullable[Seq[Volume]].inmap[Seq[Volume]](_.getOrElse(Seq.empty), x => Some(x)) and
    (JsPath \ "LxcConf").formatNullable[Seq[String]].inmap[Seq[String]](_.getOrElse(Seq.empty), x => Some(x)) and
    (JsPath \ "Privileged").formatNullable[Boolean].inmap[Boolean](_.getOrElse(false), x => Some(x)) and
    (JsPath \ "PortBindings").formatNullable[Map[Port, Seq[PortBinding]]](portBindingsObjectFormat).inmap[Map[Port, Seq[PortBinding]]](_.getOrElse(Map.empty), x => Some(x)) and
    (JsPath \ "Links").formatNullable[Seq[ContainerLink]].inmap[Seq[ContainerLink]](_.getOrElse(Seq.empty), x => Some(x)) and
    (JsPath \ "PublishAllPorts").formatNullable[Boolean].inmap[Boolean](_.getOrElse(false), x => Some(x)) and
    (JsPath \ "Dns").formatNullable[Seq[String]].inmap[Seq[String]](_.getOrElse(Seq.empty), x => Some(x)) and
    (JsPath \ "DnsSearch").formatNullable[Seq[String]].inmap[Seq[String]](_.getOrElse(Seq.empty), x => Some(x)) and
    (JsPath \ "VolumesFrom").formatNullable[Seq[String]].inmap[Seq[String]](_.getOrElse(Seq.empty), x => Some(x)) and
    (JsPath \ "Devices").formatNullable[Seq[DeviceMapping]].inmap[Seq[DeviceMapping]](_.getOrElse(Seq.empty), x => Some(x)) and
    (JsPath \ "NetworkMode").formatNullable[String].inmap[Option[String]](_.filter(_.nonEmpty), identity) and
    (JsPath \ "CapAdd").formatNullable[Seq[String]].inmap[Seq[String]](_.getOrElse(Seq.empty), x => Some(x)) and
    (JsPath \ "CapDrop").formatNullable[Seq[String]].inmap[Seq[String]](_.getOrElse(Seq.empty), x => Some(x)) and
    (JsPath \ "RestartPolicy").formatNullable[RestartPolicy].inmap[RestartPolicy](_.getOrElse(NeverRestart), x => Some(x))
    )(HostConfig.apply, unlift(HostConfig.unapply))

  implicit val containerStateFormat: Format[ContainerState] = {
    ((JsPath \ "Running").format[Boolean] and
      (JsPath \ "Paused").format[Boolean] and
      (JsPath \ "Restarting").format[Boolean] and
      (JsPath \ "Pid").format[Int] and
      (JsPath \ "ExitCode").format[Int] and
      (JsPath \ "StartedAt").format(jodaTimeOptionalStringFormat) and
      (JsPath \ "FinishedAt").format(jodaTimeOptionalStringFormat)
      )(ContainerState.apply, unlift(ContainerState.unapply))
  }

  implicit val networkSettingsFormat: Format[NetworkSettings] =
    ((JsPath \ "IPAddress").format[String] and
      (JsPath \ "IPPrefixLen").format[Int] and
      (JsPath \ "Gateway").format[String] and
      (JsPath \ "Bridge").format[String] and
      defaultMap((JsPath \ "Ports").formatNullable(portBindingsObjectFormat))
      )(NetworkSettings.apply, unlift(NetworkSettings.unapply))

  implicit val containerInfoFormat: Format[ContainerInfo] = {
    val volumesFormat: OFormat[Seq[Volume]] =
      ((JsPath \ "Volumes").format[Map[String, String]] and
        (JsPath \ "VolumesRW").format[Map[String, Boolean]]
        )({ (volumes, volumesRw) =>
        volumes.map {
          case (containerPath, hostPath) =>
            val rw = volumesRw.getOrElse(containerPath, false)
            Volume(hostPath, containerPath, rw)
        }.toSeq
      }, { volumes =>
        val v = volumes.map(v => v.containerPath -> v.hostPath).toMap
        val vRw = volumes.map(v => v.containerPath -> v.rw).toMap
        (v, vRw)
      })

    ((JsPath \ "Id").format[ContainerHashId] and
      (JsPath \ "Created").format(jodaTimeStringFormat) and
      (JsPath \ "Path").format[String] and
      (JsPath \ "Args").format[Seq[String]] and
      (JsPath \ "Config").format[ContainerConfig] and
      (JsPath \ "State").format[ContainerState] and
      (JsPath \ "Image").format[String] and
      (JsPath \ "NetworkSettings").format[NetworkSettings] and
      (JsPath \ "ResolvConfPath").format[String] and
      (JsPath \ "HostnamePath").format[String] and
      (JsPath \ "HostsPath").format[String] and
      (JsPath \ "Name").format[String] and
      (JsPath \ "Driver").format[String] and
      (JsPath \ "ExecDriver").format[String] and
      (JsPath \ "MountLabel").formatNullable[String] and
      (JsPath \ "ProcessLabel").formatNullable[String] and
      volumesFormat and
      (JsPath \ "HostConfig").format[HostConfig]
      )(ContainerInfo.apply, unlift(ContainerInfo.unapply))
  }
}
