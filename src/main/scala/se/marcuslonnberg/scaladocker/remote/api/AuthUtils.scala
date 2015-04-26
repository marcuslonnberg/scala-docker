package se.marcuslonnberg.scaladocker.remote.api

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, Uri}
import org.apache.commons.codec.binary.Base64
import play.api.libs.json.Json
import se.marcuslonnberg.scaladocker.remote.models.RegistryAuth
import se.marcuslonnberg.scaladocker.remote.models.json._

trait AuthUtils {
  this: PlayJsonSupport =>

  private[api] def auths: Seq[RegistryAuth]

  private[api] val DockerHubUrl = "https://index.docker.io/v1/"

  private[api] def getAuth(registry: Option[String]): Option[RegistryAuth] = {
    val url = registry.getOrElse(DockerHubUrl)

    def hostname(url: String) = Uri(url).authority.host.address()

    auths.find(auth => auth.url == url)
      .orElse(auths.find(auth => hostname(auth.url) == hostname(url)))
  }

  private[api] def getAuthHeader(registry: Option[String]): Option[HttpHeader] = {
    getAuth(registry).map { auth =>
      val value = {
        val json = Json.stringify(Json.toJson(auth.toConfig))
        Base64.encodeBase64String(json.getBytes("UTF-8"))
      }
      RawHeader("X-Registry-Auth", value)
    }
  }
}
