package se.marcuslonnberg.scaladocker.remote.models

import play.api.libs.json._
import se.marcuslonnberg.scaladocker.remote.models.playjson.JsonUtils._

package object playjson {
  implicit val imageIdReads = JsPath.read[String].map(ImageId)

  implicit val imageIdWrites: Writes[ImageId] = Writes { imageId =>
    JsString(imageId.hash)
  }

  implicit val containerHashIdReads = JsPath.read[String].map(ContainerHashId)

  implicit val containerHashIdWrites: Writes[ContainerHashId] = Writes { containerHashId =>
    JsString(containerHashId.hash)
  }

  implicit val imageNameReads = JsPath.read[String].map(ImageName.apply)

  implicit val imageNameWrites: Writes[ImageName] = Writes { imageName =>
    JsString(imageName.toString)
  }

  case class JsonPort(PrivatePort: Int, Type: String, PublicPort: Option[Int], IP: Option[String])

  implicit val jsonPortFormat = Json.format[JsonPort]

  implicit val portBindingsMapFormat = new Format[Map[Port, Seq[PortBinding]]] {
    override def reads(json: JsValue): JsResult[Map[Port, Seq[PortBinding]]] = {
      json.validate[Seq[JsonPort]].map { ports =>
        ports
          .map(pb => Port(pb.PrivatePort, pb.Type) -> pb)
          .collect {
          case (Some(port: Port), JsonPort(_, _, Some(hostPort), Some(hostIp))) =>
            port -> Some(PortBinding(hostIp, hostPort))
          case (Some(port: Port), _) =>
            port -> None
        }.toMapGroup.mapValues(_.flatten).toMap
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

  implicit val containerStatusFormat = JsonUtils.upperCamelCase(Json.format[ContainerStatus])

  implicit val imageFormat = JsonUtils.upperCamelCase(Json.format[Image])

}
