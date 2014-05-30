package se.marcuslonnberg.scaladocker.remote.models

import org.joda.time.DateTime

case class Image(created: DateTime,
                 id: ImageId,
                 parentId: ImageId,
                 repoTags: List[ImageName],
                 size: Long,
                 virtualSize: Long)

sealed trait ImageIdentifier

case class ImageId(hash: String) extends ImageIdentifier

object ImageName {
  def apply(name: String): ImageName = {
    val (registry, rest) = name.split("/", 3).toList match {
      case host :: x :: xs if host.contains(".") || host.contains(":") || host == "localhost" =>
        (Some(host), x :: xs)
      case host :: x :: y :: Nil =>
        throw new IllegalArgumentException(s"Invalid registry: '$host'")
      case xs =>
        (None, xs)
    }

    val (namespace, repoAndTag) = rest match {
      case n :: r :: Nil =>
        (Some(n), r)
      case r :: Nil =>
        (None, r)
    }

    val (repo, tag) = repoAndTag.split(":", 2) match {
      case Array(r, t) =>
        (r, Some(t))
      case Array(r) =>
        (r, None)
    }

    ImageName(registry, namespace, repo, tag)
  }
}

case class ImageName(registry: Option[String] = None, namespace: Option[String] = None,
                     repository: String, tag: Option[String] = None) extends ImageIdentifier {
  override def toString = {
    val registryString = registry.fold("")(_ + "/")
    val namespaceString = namespace.fold("")(_ + "/")
    val tagString = tag.fold("")(":" + _)
    registryString + namespaceString + repository + tagString
  }
}

case object NoImageName extends ImageIdentifier
