package se.marcuslonnberg.scaladocker.remote.models.json

import play.api.libs.functional.syntax._
import play.api.libs.json.JsPath
import se.marcuslonnberg.scaladocker.remote.models.{CreateContainerResponse, ContainerHashId}

trait MessagesFormats extends CommonFormats {
  implicit val createContainerResponseJson =
    ((JsPath \ "Id").format[ContainerHashId] and
      (JsPath \ "Warnings").formatNullable[Seq[String]].inmap[Seq[String]](_.getOrElse(Seq.empty), Some(_))
      )(CreateContainerResponse.apply, unlift(CreateContainerResponse.unapply))
}
