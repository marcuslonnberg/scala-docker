package se.marcuslonnberg.scaladocker.remote.models.json

import play.api.libs.functional.syntax._
import play.api.libs.json._
import se.marcuslonnberg.scaladocker.remote.models._
import se.marcuslonnberg.scaladocker.remote.models.json.JsonUtils._

trait MessagesFormats extends CommonFormats {
  implicit val createContainerResponseJson =
    ((JsPath \ "Id").format[ContainerHashId] and
      (JsPath \ "Warnings").formatWithDefault[Seq[String]](Seq.empty)
      )(CreateContainerResponse.apply, unlift(CreateContainerResponse.unapply))
}
