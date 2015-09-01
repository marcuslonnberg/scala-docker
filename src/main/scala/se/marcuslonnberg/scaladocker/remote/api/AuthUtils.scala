package se.marcuslonnberg.scaladocker.remote.api

import java.nio.file.{Files, Path}

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, Uri}
import org.apache.commons.codec.binary.Base64
import play.api.libs.json.Json
import se.marcuslonnberg.scaladocker.remote.models.json._
import se.marcuslonnberg.scaladocker.remote.models.{RegistryAuth, RegistryAuthEntry}

import scala.util.control.NonFatal

object AuthUtils {
  val DockerHubUrl = Uri("https://index.docker.io/v1/")

  def getAuth(auths: Seq[RegistryAuth], registryUri: Option[Uri] = None): Option[RegistryAuth] = {
    val uri = registryUri.getOrElse(DockerHubUrl)

    def hostname(url: Uri): String = url.authority.host.address()

    auths.find(auth => auth.url == uri)
      .orElse(auths.find(auth => hostname(auth.url) == hostname(uri)))
  }

  def getAuthHeader(auths: Seq[RegistryAuth], registryUri: Option[Uri] = None): Option[HttpHeader] = {
    getAuth(auths, registryUri).map { auth =>
      val value = {
        val json = Json.stringify(Json.toJson(auth.toConfig))
        Base64.encodeBase64String(json.getBytes("UTF-8"))
      }
      RawHeader("X-Registry-Auth", value)
    }
  }

  def readDockerCfgFile(dockerCfgPath: Path): Seq[RegistryAuth] = {
    try {
      val fileContent = new String(Files.readAllBytes(dockerCfgPath))
      val entries = Json.parse(fileContent).as[Map[String, RegistryAuthEntry]]
      entries.map {
        case (uri, entry) =>
          val decodedString = new String(Base64.decodeBase64(entry.auth))
          val Array(username, password) = decodedString.split(":", 2)
          RegistryAuth(Uri(uri), username, password)
      }.toSeq
    } catch {
      case NonFatal(ex) =>
        throw new RuntimeException(s"Failed to read Docker Registry auths from $dockerCfgPath", ex)
    }
  }
}
