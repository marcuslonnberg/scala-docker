package se.marcuslonnberg.scaladocker.remote.models

import scala.Some
import org.json4s._
import com.github.nscala_time.time.Imports._

object JsonFormats {

  def fromUpperCamelCase: PartialFunction[JField, JField] = {
    case JField(name, value) if name.nonEmpty =>
      val lowerCamelCase = name.head.toLower +: name.substring(1)
      JField(lowerCamelCase, value)
  }

  def toUpperCamelCase: PartialFunction[(String, Any), Option[(String, Any)]] = {
    case (name, value) if name.nonEmpty =>
      val upperCamelCase = name.head.toUpper +: name.substring(1)
      Some((upperCamelCase, value))
  }

  def camelCaseFieldSerializer[T](serializer: PartialFunction[(String, Any), Option[(String, Any)]] = Map(),
                                  deserializer: PartialFunction[JField, JField] = Map())(implicit mf: Manifest[T]) = {
    FieldSerializer(serializer orElse toUpperCamelCase, deserializer orElse fromUpperCamelCase)
  }

  val ImageNameFormat = new CustomSerializer[ImageName](formats => ( {
    case JString(id) => ImageName(id)
  }, {
    case name: ImageName => JString(name.name)
  }))

  val ImageIdFormat = new CustomSerializer[ImageId](formats => ( {
    case JString(id) => ImageId(id)
  }, {
    case ImageId(id) => JString(id)
  }))

  val ContainerIdFormat = new CustomSerializer[ContainerId](formats => ( {
    case JString(id) => ContainerId(id)
  }, {
    case ContainerId(id) => JString(id)
  }))

  val ContainerLinkFormat = new CustomSerializer[ContainerLink](formats => ( {
    case JString(link) => ContainerLink.parse(link)
  }, {
    case link: ContainerLink => JString(link.link)
  }))

  val DateTimeFormat = new CustomSerializer[DateTime](formats => ( {
    case JInt(seconds) => new DateTime((seconds * 1000).toLong)
  }, {
    case dateTime: DateTime => JInt(dateTime.millis / 1000)
  }))

  def apply(): Formats = DefaultFormats +
    camelCaseFieldSerializer[Ports](deserializer = FieldSerializer.renameFrom("IP", "ip")) +
    camelCaseFieldSerializer[ContainerStatus]() +
    camelCaseFieldSerializer[Image]() +
    camelCaseFieldSerializer[ContainerConfig]() +
    camelCaseFieldSerializer[HostConfig]() +
    camelCaseFieldSerializer[CreateContainerResponse]() +
    ImageNameFormat +
    ImageIdFormat +
    ContainerIdFormat +
    ContainerLinkFormat +
    DateTimeFormat
}
