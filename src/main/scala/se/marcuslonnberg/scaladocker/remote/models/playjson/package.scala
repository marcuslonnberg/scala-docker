package se.marcuslonnberg.scaladocker.remote.models

import org.joda.time.DateTime
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json._
import se.marcuslonnberg.scaladocker.remote.models.playjson.JsonUtils._

package object playjson {
  implicit val dateTimeSecondsFormat = Format[DateTime](
    JsPath.read[Long].map(seconds => new DateTime(seconds * 1000)),
    Writes { dt =>
      JsNumber(dt.getMillis / 1000)
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

  case class JsonPort(PrivatePort: Int, Type: String, PublicPort: Option[Int], IP: Option[String])

  implicit val jsonPortFormat = Json.format[JsonPort]

  implicit val portBindingsArrayFormat = new Format[Map[Port, Seq[PortBinding]]] {
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

  implicit val portBindingsObjectFormat = Format[Seq[Port]](Reads { in =>
    in.validate[JsObject].map { obj =>
      obj.fields.flatMap(f => Port.unapply(f._1).toSeq)
    }
  }, Writes { in =>
    val fields = in.map(port => port.toString -> JsObject(Seq.empty))
    JsObject(fields)
  })

  implicit val containerStatusFormat = JsonUtils.upperCamelCase(Json.format[ContainerStatus])

  implicit val imageFormat = JsonUtils.upperCamelCase(Json.format[Image])

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
}
