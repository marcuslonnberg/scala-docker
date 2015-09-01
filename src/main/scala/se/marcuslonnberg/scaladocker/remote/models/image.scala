package se.marcuslonnberg.scaladocker.remote.models

import org.joda.time.DateTime

case class Image(
  created: DateTime,
  id: ImageId,
  parentId: ImageId,
  repoTags: Seq[ImageName],
  labels: Map[String, String],
  size: Long,
  virtualSize: Long
)

sealed trait ImageIdentifier

case class ImageId(hash: String) extends ImageIdentifier {
  def shortHash = hash.take(12)

  override def toString = hash
}

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
        (r, t)
      case Array(r) =>
        (r, "latest")
    }

    ImageName(registry, namespace, repo, tag)
  }
}

case class ImageName(registry: Option[String] = None, namespace: Option[String] = None,
  repository: String, tag: String = "latest") extends ImageIdentifier {
  namespace.foreach { n =>
    require(n.matches("[a-z0-9-_]{4,30}"),
      s"Namespace name ('$n') can only contain characters [a-z0-9_] and have a size between 4 and 30")
  }
  require(repository.matches("[a-z0-9-_.]+") || repository == "<none>",
    s"Repository name ('$repository') can only contain characters [a-z0-9-_.]")
  require(tag.matches("[\\w][\\w.-]{0,127}") || tag == "<none>",
    s"Tag name ('$tag') can only contain characters [A-Za-z0-9_.-] and have a length between 1 and 128")

  override def toString = {
    nameWithoutTag + ":" + tag
  }

  def nameWithoutTag: String = {
    val registryString = registry.fold("")(_ + "/")
    val namespaceString = namespace.fold("")(_ + "/")
    registryString + namespaceString + repository
  }
}

case object MissingImageName extends ImageIdentifier {
  override def toString = "<none>"
}
