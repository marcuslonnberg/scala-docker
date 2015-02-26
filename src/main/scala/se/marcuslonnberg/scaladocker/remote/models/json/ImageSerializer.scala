package se.marcuslonnberg.scaladocker.remote.models.json

import com.github.nscala_time.time.Imports._
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import org.json4s._
import se.marcuslonnberg.scaladocker.remote.models.JsonFormatHelpers._
import se.marcuslonnberg.scaladocker.remote.models._

object ImageSerializer extends CustomSerializer[Image](implicit formats => ( {
  case obj: JObject =>
    implicit val o = obj

    val created = new DateTime(extractField[Long]("Created") * 1000)
    val id = extractField[ImageId]("Id")
    val parentId = extractField[ImageId]("ParentId")
    val repoTags = extractFieldList[ImageName]("RepoTags")
    val size = extractField[Long]("Size")
    val virtualSize = extractField[Long]("VirtualSize")

    Image(
      created = created,
      id = id,
      parentId = parentId,
      repoTags = repoTags,
      size = size,
      virtualSize = virtualSize)
}, {
  case image: Image =>
    ("Created" -> image.created.getMillis / 1000) ~
      ("Id" -> image.id.hash) ~
      ("ParentId" -> image.parentId.hash) ~
      ("RepoTags" -> Extraction.decompose(image.repoTags)) ~
      ("Size" -> image.size) ~
      ("VirtualSize" -> image.virtualSize)
}))
