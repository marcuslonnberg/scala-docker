package se.marcuslonnberg.scaladocker.remote.api

import java.io._

import akka.http.model.Uri.Path
import akka.http.model._
import akka.stream.scaladsl.{Sink, Source}
import org.reactivestreams.Publisher
import se.marcuslonnberg.scaladocker.remote.models.{BuildMessage, ImageName}

trait BuildCommand extends DockerCommands {
  def build(imageName: ImageName, tarFile: File, noCache: Boolean = false, rm: Boolean = true): Publisher[BuildMessage] = {
    val query = Uri.Query(
      "t" -> imageName.toString,
      "nocache" -> noCache.toString,
      "rm" -> rm.toString)
    val uri = createUri(Path / "build", query)

    val entity = HttpEntity(ContentType(MediaType.custom("application/tar")), readBytes(tarFile))
    val request = HttpRequest(HttpMethods.POST, uri, entity = entity)

    Source(requestChunkedLines(request))
      .filter(_.nonEmpty)
      .map { line =>
      val maybeMessage: Option[BuildMessage] = ???
      maybeMessage
    }.collect {
      case Some(v) => v
    }.runWith(Sink.publisher[BuildMessage])
  }

  private def readBytes(file: File): Array[Byte] = {
    val ra = new RandomAccessFile(file, "r")
    val bytes = Array.ofDim[Byte](ra.length().toInt)
    ra.read(bytes)
    bytes
  }
}
