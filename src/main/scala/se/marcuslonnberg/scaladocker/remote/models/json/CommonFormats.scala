package se.marcuslonnberg.scaladocker.remote.models.json

import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatterBuilder, DateTimeFormat, ISODateTimeFormat}
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json._
import se.marcuslonnberg.scaladocker.remote.models._
import se.marcuslonnberg.scaladocker.remote.models.json.JsonUtils._

import scala.util.Try

trait CommonFormats {
  val dateTimeSecondsFormat: Format[DateTime] = Format[DateTime](
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

  implicit val imageIdFormat: Format[ImageId] = Format[ImageId](
    JsPath.read[String].map(ImageId),
    Writes { imageId =>
      JsString(imageId.hash)
    }
  )

  implicit val containerHashIdFormat: Format[ContainerHashId] = Format[ContainerHashId](
    JsPath.read[String].map(ContainerHashId),
    Writes { containerHashId =>
      JsString(containerHashId.hash)
    }
  )

  implicit val imageNameFormat: Format[ImageName] = Format[ImageName](
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

  val portBindingsArrayFormat: Format[Map[Port, Seq[PortBinding]]] = new Format[Map[Port, Seq[PortBinding]]] {
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

  def defaultMap[K, V](format: OFormat[Option[Map[K, V]]]): OFormat[Map[K, V]] =
    format.inmap({ i =>
      i.getOrElse(Map.empty)
    }, { i => Some(i) })

  val portBindingsObjectFormat: Format[Map[Port, Seq[PortBinding]]] = {
    JsPath.format[Map[String, Seq[PortBinding]]].inmap[Map[Port, Seq[PortBinding]]](m => m.map {
      case (Port(port), bindings) => port -> bindings
    }, m => m.map {
      case (port, bindings) => port.toString -> bindings
    })
  }

  implicit val portBindingsEmptyObjectFormat: Format[Seq[Port]] = Format[Seq[Port]](Reads { in =>
    in.validate[JsObject].map { obj =>
      obj.fields.map {
        case (Port(port), _) => port
      }
    }
  }, Writes { in =>
    val fields = in.map(port => port.toString -> JsObject(Seq.empty))
    JsObject(fields)
  })

}

object CommonFormats extends CommonFormats
