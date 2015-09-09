package se.marcuslonnberg.scaladocker.remote.api

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.{Http, HttpsContext}
import akka.stream.Materializer
import se.marcuslonnberg.scaladocker.remote.models.RegistryAuth

import scala.concurrent.Future

object DockerConnection {
  def fromEnvironment()(implicit system: ActorSystem, materializer: Materializer): DockerConnection = {
    def env(key: String): Option[String] = sys.env.get(key).filter(_.nonEmpty)

    val host = env("DOCKER_HOST").getOrElse {
      sys.error(s"Environment variable DOCKER_HOST is not set")
    }
    val maybeTls = env("DOCKER_TLS_VERIFY").filterNot(v => v == "0" || v == "false").flatMap { _ =>
      env("DOCKER_CERT_PATH").map(TlsConfig.fromDir)
    }

    val auths = sys.props.get("user.home").flatMap { homePath =>
      val dockerCfgPath = Paths.get(homePath, ".dockercfg")
      if (dockerCfgPath.toFile.isFile) {
        Some(AuthUtils.readDockerCfgFile(dockerCfgPath))
      } else {
        None
      }
    }.getOrElse(Seq.empty)

    maybeTls match {
      case Some(tls) =>
        DockerHttpsConnection(Uri(host.replaceFirst("tcp:", "https:")), tls, auths)
      case _ =>
        DockerHttpConnection(host.replaceFirst("tcp:", "http:"), auths)
    }
  }
}

trait DockerConnection {
  def baseUri: Uri

  def auths: Seq[RegistryAuth]

  def system: ActorSystem

  implicit def materializer: Materializer

  def sendRequest(request: HttpRequest): Future[HttpResponse]

  def buildUri(path: Path, query: Query = Query.Empty): Uri = {
    baseUri.copy(path = baseUri.path ++ path, query = query)
  }
}

case class DockerHttpsConnection(
  baseUri: Uri,
  tls: TlsConfig,
  auths: Seq[RegistryAuth]
)(implicit val system: ActorSystem, val materializer: Materializer)
  extends DockerConnection with TlsSupport {

  val httpsContext = HttpsContext(createSSLContext(tls))

  override def sendRequest(request: HttpRequest): Future[HttpResponse] = {
    Http(system).singleRequest(request, httpsContext = Some(httpsContext))
  }
}

case class DockerHttpConnection(
  baseUri: Uri,
  auths: Seq[RegistryAuth]
)(implicit val system: ActorSystem, val materializer: Materializer)
  extends DockerConnection {
  override def sendRequest(request: HttpRequest): Future[HttpResponse] = {
    Http(system).singleRequest(request)
  }
}

