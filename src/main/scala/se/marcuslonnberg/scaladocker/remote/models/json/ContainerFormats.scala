package se.marcuslonnberg.scaladocker.remote.models.json

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import se.marcuslonnberg.scaladocker.remote.models._
import se.marcuslonnberg.scaladocker.remote.models.json.JsonUtils._

trait ContainerFormats extends CommonFormats {

  implicit val nodeFormat: Format[Node] = {
    ((JsPath \ "Labels").formatWithDefault[Map[String, String]](Map.empty) and
      (JsPath \ "Memory").format[Long] and
      (JsPath \ "Cpus").format[Long] and
      (JsPath \ "Name").format[String] and
      (JsPath \ "Address").format[String] and
      (JsPath \ "Ip").format[String] and
      (JsPath \ "Id").format[String]
      ) (Node.apply, unlift(Node.unapply))
  }

  implicit val containerStatusFormat = {
    ((JsPath \ "Command").format[String] and
      (JsPath \ "Created").format[DateTime](dateTimeSecondsFormat) and
      (JsPath \ "Id").format[ContainerHashId] and
      (JsPath \ "Image").format[ImageName] and
      (JsPath \ "Names").formatWithDefault[Seq[String]](Seq.empty) and
      (JsPath \ "Ports").formatWithDefault[Map[Port, Seq[PortBinding]]](Map.empty)(portBindingsArrayFormat) and
      (JsPath \ "Labels").formatWithDefault[Map[String, String]](Map.empty) and
      (JsPath \ "Status").format[String] and
      (JsPath \ "Node").formatNullable[Node](nodeFormat)
      ) (ContainerStatus.apply, unlift(ContainerStatus.unapply))
  }

  implicit val volumeBindingFormat = Format[VolumeBinding](Reads { in =>
    in.validate[String].flatMap {
      case VolumeBinding(binding) =>
        JsSuccess(binding)
      case binding =>
        JsError(s"Could not parse binding: $binding")
    }
  }, Writes { volume =>
    val maybeRw = if (volume.rw) None else Some("ro")
    JsString((volume.hostPath :: volume.containerPath :: maybeRw.toList).mkString(":"))
  })

  implicit val standardStreamsConfigFormat: OFormat[StandardStreamsConfig] =
    ((JsPath \ "AttachStdin").formatWithDefault[Boolean](false) and
      (JsPath \ "AttachStdout").formatWithDefault[Boolean](false) and
      (JsPath \ "AttachStderr").formatWithDefault[Boolean](false) and
      (JsPath \ "Tty").formatWithDefault[Boolean](false) and
      (JsPath \ "OpenStdin").formatWithDefault[Boolean](false) and
      (JsPath \ "StdinOnce").formatWithDefault[Boolean](false)
      ) (StandardStreamsConfig.apply, unlift(StandardStreamsConfig.unapply))

  implicit val containerResourceLimitsFormat: OFormat[ContainerResourceLimits] =
    ((JsPath \ "Memory").formatWithDefault[Long](0) and
      (JsPath \ "MemorySwap").formatWithDefault[Long](0) and
      (JsPath \ "CpuShares").formatWithDefault[Long](0) and
      (JsPath \ "Cpuset").formatNullable[String]
      ) (ContainerResourceLimits.apply, unlift(ContainerResourceLimits.unapply))

  implicit val containerConfigFormat: OFormat[ContainerConfig] =
    ((JsPath \ "Image").format[ImageName] and
      (JsPath \ "Entrypoint").formatNullable[Seq[String]] and
      (JsPath \ "Cmd").formatWithDefault[Seq[String]](Seq.empty) and
      (JsPath \ "Env").formatWithDefault[Seq[String]](Seq.empty) and
      (JsPath \ "ExposedPorts").formatWithDefault[Seq[Port]](Seq.empty) and
      (JsPath \ "Volumes").formatWithDefault[Map[String, JsObject]](Map.empty)
        .inmap[Seq[String]](_.keys.toSeq, _.map(_ -> JsObject(Seq.empty)).toMap) and
      (JsPath \ "WorkingDir").formatNullable[String] and
      (JsPath \ "User").formatNullable[String] and
      (JsPath \ "Hostname").formatNullable[String] and
      (JsPath \ "DomainName").formatNullable[String] and
      containerResourceLimitsFormat and
      standardStreamsConfigFormat and
      (JsPath \ "Labels").formatWithDefault[Map[String, String]](Map.empty) and
      (JsPath \ "NetworkDisabled").formatWithDefault[Boolean](false)
      ) (ContainerConfig.apply, unlift(ContainerConfig.unapply))

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

  implicit val capabilitiesConfigFormat: OFormat[LinuxCapabilities] =
    ((JsPath \ "CapAdd").formatWithDefault[Seq[String]](Seq.empty) and
      (JsPath \ "CapDrop").formatWithDefault[Seq[String]](Seq.empty)
      ) (LinuxCapabilities.apply, unlift(LinuxCapabilities.unapply))

  implicit val hostConfigFormat: Format[HostConfig] =
    ((JsPath \ "PortBindings").formatWithDefault[Map[Port, Seq[PortBinding]]](Map.empty)(portBindingsObjectFormat) and
      (JsPath \ "PublishAllPorts").formatWithDefault[Boolean](false) and
      (JsPath \ "Links").formatWithDefault[Seq[ContainerLink]](Seq.empty) and
      (JsPath \ "Binds").formatWithDefault[Seq[VolumeBinding]](Seq.empty) and
      (JsPath \ "VolumesFrom").formatWithDefault[Seq[String]](Seq.empty) and
      (JsPath \ "Devices").formatWithDefault[Seq[DeviceMapping]](Seq.empty) and
      (JsPath \ "ReadonlyRootfs").formatWithDefault[Boolean](false) and
      (JsPath \ "Dns").formatWithDefault[Seq[String]](Seq.empty) and
      (JsPath \ "DnsSearch").formatWithDefault[Seq[String]](Seq.empty) and
      (JsPath \ "NetworkMode").formatNullable[String].inmap[Option[String]](_.filter(_.nonEmpty), identity) and
      (JsPath \ "Privileged").formatWithDefault[Boolean](false) and
      capabilitiesConfigFormat and
      (JsPath \ "RestartPolicy").formatWithDefault[RestartPolicy](NeverRestart)
      ) (HostConfig.apply, unlift(HostConfig.unapply))

  implicit val containerStateFormat: Format[ContainerState] = {
    ((JsPath \ "Running").format[Boolean] and
      (JsPath \ "Paused").format[Boolean] and
      (JsPath \ "Restarting").format[Boolean] and
      (JsPath \ "Pid").format[Int] and
      (JsPath \ "ExitCode").format[Int] and
      (JsPath \ "StartedAt").format(jodaTimeOptionalStringFormat) and
      (JsPath \ "FinishedAt").format(jodaTimeOptionalStringFormat)
      ) (ContainerState.apply, unlift(ContainerState.unapply))
  }

  implicit val networkSettingsFormat: Format[NetworkSettings] =
    ((JsPath \ "IPAddress").format[String] and
      (JsPath \ "IPPrefixLen").format[Int] and
      (JsPath \ "Gateway").format[String] and
      (JsPath \ "Bridge").format[String] and
      (JsPath \ "Ports").formatWithDefault[Map[Port, Seq[PortBinding]]](Map.empty)(portBindingsObjectFormat)
      ) (NetworkSettings.apply, unlift(NetworkSettings.unapply))

  implicit val containerInfoFormat: Format[ContainerInfo] = {
    val volumesFormat: OFormat[Seq[VolumeBinding]] =
      ((JsPath \ "Volumes").formatWithDefault[Map[String, String]](Map.empty) and
        (JsPath \ "VolumesRW").formatWithDefault[Map[String, Boolean]](Map.empty)
        ) ({ (volumes, volumesRw) =>
        volumes.map {
          case (containerPath, hostPath) =>
            val rw = volumesRw.getOrElse(containerPath, false)
            VolumeBinding(hostPath, containerPath, rw)
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
      (JsPath \ "MountLabel").formatNullable[String] and
      (JsPath \ "ProcessLabel").formatNullable[String] and
      volumesFormat and
      (JsPath \ "HostConfig").format[HostConfig]
      ) (ContainerInfo.apply, unlift(ContainerInfo.unapply))
  }
}
