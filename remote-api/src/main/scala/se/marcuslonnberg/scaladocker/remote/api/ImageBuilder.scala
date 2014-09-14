package se.marcuslonnberg.scaladocker.remote.api

import java.io._
import java.nio.file.Files

import akka.http.model.Uri.Path
import akka.http.model._
import akka.stream.scaladsl2.FlowFrom
import org.json4s.JObject
import org.json4s.native.Serialization._
import org.kamranzafar.jtar.{TarEntry, TarOutputStream}
import org.reactivestreams.Publisher
import se.marcuslonnberg.scaladocker.remote.models.{BuildMessage, BuildMessages, ImageName}

trait BuildCommand extends DockerCommands {
  def build(imageName: ImageName, tarFile: File): Publisher[BuildMessage] = {
    val query = Uri.Query(
      "t" -> imageName.toString)
    val uri = createUri(Path / "build", query)

    val entity = HttpEntity(ContentType(MediaType.custom("application/tar")), Files.readAllBytes(tarFile.toPath))
    val request = HttpRequest(HttpMethods.POST, uri, entity = entity)

    FlowFrom(requestChunkedLines(request))
      .filter(_.nonEmpty)
      .map { line =>
      val obj = read[JObject](line)
      val maybeMessage: Option[BuildMessage] = obj.extractOpt[BuildMessages.Output]
        .orElse(obj.extractOpt[BuildMessages.Error])
      maybeMessage
    }.collect {
      case Some(v) => v
    }.toPublisher()
  }
}

object TarArchive {
  def apply(files: Map[String, File], outFile: File) = {
    val tarFile = new FileOutputStream(outFile)
    val tarStream = new TarOutputStream(new BufferedOutputStream(tarFile))

    val buffer = Array.ofDim[Byte](2048)

    def copyFile(file: File) = {
      val fileStream = new BufferedInputStream(new FileInputStream(file))

      def copy(input: InputStream) {
        val len = input.read(buffer)
        if (len != -1) {
          tarStream.write(buffer, 0, len)
          copy(input)
        }
      }
      copy(fileStream)

      fileStream.close()
    }

    files.foreach {
      case (path, file) =>
        tarStream.putNextEntry(new TarEntry(file, path))

        copyFile(file)

        tarStream.flush()
    }

    tarStream.close()
    tarFile.close()
  }
}
