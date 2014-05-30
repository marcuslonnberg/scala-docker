package se.marcuslonnberg.scaladocker.remote.models

import com.github.nscala_time.time.Imports._
import org.joda.time.format.ISODateTimeFormat
import org.json4s.FieldSerializer._
import org.json4s._

object JsonFormats {

  def lowerCamelCase(name: String) = name.head.toLower +: name.substring(1)

  def fromUpperCamelCase: PartialFunction[JField, JField] = {
    case JField(name, value) if name.nonEmpty =>
      JField(lowerCamelCase(name), value)
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
    case name: ImageName => JString(name.toString)
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
    case JString(date) => ISODateTimeFormat.dateTime().parseDateTime(date)
  }, {
    case dateTime: DateTime => JInt(dateTime.millis / 1000)
  }))

  val OptionStringFormat = new CustomSerializer[Option[String]](formats => ( {
    case JString("") => None
    case JString(string) => Some(string)
  }, {
    case None => JNothing
    case Some(string: String) => JString(string)
  }))

  def apply(): Formats = DefaultFormats +
    camelCaseFieldSerializer[Port](deserializer = renameFrom("IP", "ip")) +
    camelCaseFieldSerializer[ContainerStatus]() +
    camelCaseFieldSerializer[Image]() +
    camelCaseFieldSerializer[ContainerConfig]() +
    camelCaseFieldSerializer[HostConfig]() +
    camelCaseFieldSerializer[CreateContainerResponse]() +
    camelCaseFieldSerializer[ContainerState]() +
    camelCaseFieldSerializer[NetworkSettings](deserializer = renameFrom("IPAddress", "ipAddress") orElse
      renameFrom("IPPrefixLen", "ipPrefixLen")) +
    camelCaseFieldSerializer[ContainerInfo](deserializer = renameFrom("ID", "id")) +
    ImageNameFormat +
    ImageIdFormat +
    ContainerIdFormat +
    ContainerLinkFormat +
    DateTimeFormat +
    OptionStringFormat

  def objectKeysToArray(fieldName: String) = PartialFunction[JField, JField] {
    case JField(`fieldName`, volumes) =>
      JField(lowerCamelCase(fieldName), JArray(volumes.asInstanceOf[JObject].obj.map(v => JString(v._1))))
  }
}
