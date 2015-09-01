package se.marcuslonnberg.scaladocker.remote.models.json

import play.api.libs.functional.syntax._
import play.api.libs.json._
import se.marcuslonnberg.scaladocker.remote.models._
import se.marcuslonnberg.scaladocker.remote.models.json.JsonUtils._

trait MessagesFormats extends CommonFormats with CreateImageMessageFormats with BuildMessageFormat with RemoveImageMessageFormats {
  implicit val createContainerResponseJson =
    ((JsPath \ "Id").format[ContainerHashId] and
      (JsPath \ "Warnings").formatWithDefault[Seq[String]](Seq.empty)
      )(CreateContainerResponse.apply, unlift(CreateContainerResponse.unapply))
}

trait BuildMessageFormat extends CommonFormats {

  import se.marcuslonnberg.scaladocker.remote.models.BuildMessages._

  implicit val buildMessageOutputFormat = JsonUtils.upperCamelCase(Json.format[Output])
  implicit val buildMessageErrorDetailFormat = JsonUtils.upperCamelCase(Json.format[ErrorDetail])
  implicit val buildMessageErrorFormat = JsonUtils.upperCamelCase(Json.format[Error])

  implicit val buildMessageFormat: Format[BuildMessage] = Format(Reads { in =>
    buildMessageOutputFormat.reads(in)
      .or(buildMessageErrorFormat.reads(in))
  }, Writes[BuildMessage] {
    case v: Output => buildMessageOutputFormat.writes(v)
    case v: Error => buildMessageErrorFormat.writes(v)
  })

}

trait CreateImageMessageFormats extends CommonFormats {

  import se.marcuslonnberg.scaladocker.remote.models.ImageTransferMessage._

  implicit val createImageMessageStatusFormat = JsonUtils.upperCamelCase(Json.format[Status])
  implicit val createImageMessageImageStatusFormat = JsonUtils.upperCamelCase(Json.format[ImageStatus])
  implicit val createImageMessageProgressDetailFormat = JsonUtils.upperCamelCase(Json.format[ProgressDetail])
  implicit val createImageMessageProgressFormat = JsonUtils.upperCamelCase(Json.format[Progress])
  implicit val createImageMessageErrorFormat = JsonUtils.upperCamelCase(Json.format[Error])

  implicit val createImageMessageFormat: Format[ImageTransferMessage] = Format(Reads { in =>
    createImageMessageProgressFormat.reads(in)
      .or(createImageMessageErrorFormat.reads(in))
      .or(createImageMessageImageStatusFormat.reads(in))
      .or(createImageMessageStatusFormat.reads(in))
  }, Writes[ImageTransferMessage] {
    case v: Status => createImageMessageStatusFormat.writes(v)
    case v: ImageStatus => createImageMessageImageStatusFormat.writes(v)
    case v: Progress => createImageMessageProgressFormat.writes(v)
    case v: Error => createImageMessageErrorFormat.writes(v)
  })
}

trait RemoveImageMessageFormats extends CommonFormats {

  import RemoveImageMessage._

  implicit val removeImageMessageUntaggedFormat = (JsPath \ "Untagged").format[String]
    .inmap[Untagged](in => Untagged(ImageName(in)), _.imageName.toString)
  implicit val removeImageMessageDeletedFormat = (JsPath \ "Deleted").format[String]
    .inmap[Deleted](in => Deleted(ImageId(in)), _.imageId.toString)

  implicit val removeImageMessageFormat: Format[RemoveImageMessage] = Format(Reads { in =>
    removeImageMessageUntaggedFormat.reads(in)
      .or(removeImageMessageDeletedFormat.reads(in))
  }, Writes[RemoveImageMessage] {
    case v: Deleted => removeImageMessageDeletedFormat.writes(v)
    case v: Untagged => removeImageMessageUntaggedFormat.writes(v)
  })
}
