package se.marcuslonnberg.scaladocker.remote.models.json

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import se.marcuslonnberg.scaladocker.remote.models.json.JsonUtils._
import se.marcuslonnberg.scaladocker.remote.models.{Image, ImageId, ImageName}

trait ImageFormats extends CommonFormats {
  implicit val imageFormat = {
    ((JsPath \ "Created").format[DateTime](dateTimeSecondsFormat) and
      (JsPath \ "Id").format[ImageId] and
      (JsPath \ "ParentId").format[ImageId] and
      (JsPath \ "RepoTags").formatWithDefault[Seq[ImageName]](Seq.empty) and
      (JsPath \ "Labels").formatWithDefault[Map[String, String]](Map.empty) and
      (JsPath \ "Size").format[Long] and
      (JsPath \ "VirtualSize").format[Long]
      )(Image.apply, unlift(Image.unapply))
  }
}
