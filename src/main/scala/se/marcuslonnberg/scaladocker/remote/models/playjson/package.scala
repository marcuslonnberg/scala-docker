package se.marcuslonnberg.scaladocker.remote.models

import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatterBuilder, ISODateTimeFormat}
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json._
import se.marcuslonnberg.scaladocker.remote.models.playjson.JsonUtils._

import scala.util.Try

package object playjson {
  val dateTimeSecondsFormat = Format[DateTime](
    JsPath.read[Long].map(seconds => new DateTime(seconds * 1000)),
    Writes { dt =>
      JsNumber(dt.getMillis / 1000)
    }
  )

  val jodaTimeStringFormat: Format[DateTime] = {
    val formatter = {
      val parsers = Array(
        ISODateTimeFormat.basicDateTime,
        DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZZ")
      ).map(_.getParser)
      new DateTimeFormatterBuilder().append(null, parsers).toFormatter
    }

    Format(
      JsPath.read[String].map(x => formatter.parseDateTime(x)),
      Writes { in =>
        JsString(in.toString)
      }
    )
  }

  implicit val jodaTimeOptionalStringFormat: Format[Option[DateTime]] = {
    val formatter = ISODateTimeFormat.basicDateTime

    Format(
      JsPath.format[String].map({
        case "0001-01-01T00:00:00Z" => None
        case input => Try(formatter.parseDateTime(input)).toOption
      }), Writes { x =>
        val v = x.map(_.toString).getOrElse("0001-01-01T00:00:00Z")
        JsString(v)
      })
  }

  def nullableSeqFormat[T: Format] = Format[Seq[T]](
    JsPath.readNullable[Seq[T]].map({ x =>
      x.getOrElse(Seq.empty)
    }), Writes { x =>
      JsArray(x.map(Json.toJson(_)))
    }
  )

  implicit val imageIdFormat = Format[ImageId](
    JsPath.read[String].map(ImageId),
    Writes { imageId =>
      JsString(imageId.hash)
    }
  )

  implicit val containerHashIdFormat = Format[ContainerHashId](
    JsPath.read[String].map(ContainerHashId),
    Writes { containerHashId =>
      JsString(containerHashId.hash)
    }
  )

  implicit val imageNameFormat = Format[ImageName](
    JsPath.read[String].map(ImageName.apply),
    Writes { imageName =>
      JsString(imageName.toString)
    }
  )

  implicit val portFormat = Format[Port](
    JsPath.read[String].collect(ValidationError("Bad port format")) {
      case Port(port) => port
    },
    Writes { port =>
      JsString(port.toString)
    }
  )

  implicit val portBindingFormat: Format[PortBinding] =
    ((JsPath \ "HostIp").format[String] and
      (JsPath \ "HostPort").format[String].inmap[Int](_.toInt, _.toString)
      )(PortBinding.apply, unlift(PortBinding.unapply))

  case class JsonPort(PrivatePort: Int, Type: String, PublicPort: Option[Int], IP: Option[String])

  implicit val jsonPortFormat = Json.format[JsonPort]

  val portBindingsArrayFormat = new Format[Map[Port, Seq[PortBinding]]] {
    override def reads(json: JsValue): JsResult[Map[Port, Seq[PortBinding]]] = {
      json.validate[Seq[JsonPort]].map { ports =>
        ports
          .map(pb => Port(pb.PrivatePort, pb.Type) -> pb)
          .collect {
          case (Some(port: Port), JsonPort(_, _, Some(hostPort), Some(hostIp))) =>
            port -> Some(PortBinding(hostIp, hostPort))
          case (Some(port: Port), _) =>
            port -> None
        }.toMapGroup.map {
          case (k, v) => k -> v.flatten
        }
      }
    }

    override def writes(o: Map[Port, Seq[PortBinding]]): JsValue = {
      val ports = o.toSeq.map {
        case (port, Nil) =>
          Seq(JsonPort(port.port, port.protocol, None, None))
        case (port, bindings) =>
          bindings.map { binding =>
            JsonPort(port.port, port.protocol, Some(binding.hostPort), Some(binding.hostIp))
          }
      }.flatten
      JsArray(ports.map(Json.toJson(_)))
    }
  }

  implicit def defaultMap[K, V](format: OFormat[Option[Map[K, V]]]): OFormat[Map[K, V]] =
  format.inmap({ i =>
    i.getOrElse(Map.empty)
  }, {i => Some(i)})

  val portBindingsObjectFormat: Format[Map[Port, Seq[PortBinding]]] = {
    JsPath.format[Map[String, Seq[PortBinding]]].inmap[Map[Port, Seq[PortBinding]]](m => m.map {
      case (Port(port), bindings) => port -> bindings
    }, m => m.map {
      case (port, bindings) => port.toString -> bindings
    })
  }

  implicit val portBindingsEmptyObjectFormat = Format[Seq[Port]](Reads { in =>
    in.validate[JsObject].map { obj =>
      obj.fields.map {
        case (Port(port), _) => port
      }
    }
  }, Writes { in =>
    val fields = in.map(port => port.toString -> JsObject(Seq.empty))
    JsObject(fields)
  })

  implicit val containerStatusFormat = {
    implicit val a = portBindingsArrayFormat
    implicit val b = dateTimeSecondsFormat
    JsonUtils.upperCamelCase(Json.format[ContainerStatus])
  }

  implicit val imageFormat = {
    implicit val a = dateTimeSecondsFormat
    JsonUtils.upperCamelCase(Json.format[Image])
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
    val rwSeq = if (volume.rw) Seq.empty else Seq("ro")
    JsString((Seq(volume.hostPath, volume.containerPath) ++ rwSeq).mkString(":"))
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
        ) { (image: ImageName, hostname: Option[String], domainName: Option[String], user: Option[String], memory: Long, memorySwap: Long, cpuShares: Long, cpuset: Option[String], attachStdin: Boolean, attachStdout: Boolean, attachStderr: Boolean, exposedPorts: Seq[Port], env: Seq[String], cmd: Seq[String], volumes: Seq[String], workingDir: Option[String], entryPoint: Seq[String], networkDisabled: Boolean, tty: Boolean, openStdin: Boolean, onBuild: Seq[String]) =>

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

  implicit val createContainerResponseJson =
    ((JsPath \ "Id").format[ContainerHashId] and
      (JsPath \ "Warnings").formatNullable[Seq[String]].inmap[Seq[String]](_.getOrElse(Seq.empty), Some(_))
      )(CreateContainerResponse.apply, unlift(CreateContainerResponse.unapply))
}
