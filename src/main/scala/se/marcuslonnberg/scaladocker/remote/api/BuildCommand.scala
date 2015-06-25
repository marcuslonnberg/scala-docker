package se.marcuslonnberg.scaladocker.remote.api

import java.io._

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Sink, Source}
import org.reactivestreams.Publisher
import se.marcuslonnberg.scaladocker.remote.models.json._
import se.marcuslonnberg.scaladocker.remote.models.{BuildMessage, ImageName}

trait BuildCommand extends DockerCommands {
  def build(tarFile: File, imageName: Option[ImageName] = None, noCache: Boolean = false, rm: Boolean = true): Publisher[BuildMessage] = {
    val query = Uri.Query(Map(
      "t" -> imageName.map(_.toString),
      "nocache" -> Some(noCache.toString),
      "rm" -> Some(rm.toString))
      .collect { case (key, Some(value)) => key -> value })
    val uri = createUri(Path / "build", query)

    val entity = HttpEntity(ContentType(MediaTypes.`application/x-tar`), readBytes(tarFile))
    val request = HttpRequest(HttpMethods.POST, uri, entity = entity)

    Source(requestChunkedLinesJson[BuildMessage](request))
      .runWith(Sink.publisher[BuildMessage])
  }

  private def readBytes(file: File): Array[Byte] = {
    val ra = new RandomAccessFile(file, "r")
    val bytes = Array.ofDim[Byte](ra.length().toInt)
    ra.read(bytes)
    bytes
  }
}
