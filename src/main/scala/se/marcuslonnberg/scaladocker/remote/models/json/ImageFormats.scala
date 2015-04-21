package se.marcuslonnberg.scaladocker.remote.models.json

import play.api.libs.json.Json
import se.marcuslonnberg.scaladocker.remote.models.Image

trait ImageFormats extends CommonFormats{
  implicit val imageFormat = {
    implicit val a = dateTimeSecondsFormat
    JsonUtils.upperCamelCase(Json.format[Image])
  }
}
