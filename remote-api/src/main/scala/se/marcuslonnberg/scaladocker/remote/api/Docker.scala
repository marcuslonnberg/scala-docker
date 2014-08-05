package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorSystem
import org.json4s.JObject
import org.json4s.native.Serialization._
import se.marcuslonnberg.scaladocker.remote.models._
import spray.http.HttpMethods.{DELETE, POST}
import spray.http.Uri._
import spray.http._
import spray.httpx.UnsuccessfulResponseException

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

case class DockerClient(hostname: String, port: Int = 4243)(implicit val system: ActorSystem) {
  val containers = new ContainerCommands with Context
  val host = new HostCommands with Context
  val images = new ImageCommands with Context

  trait Context {
    this: DockerCommands =>
    override def baseUri = Uri(s"http://$hostname:$port")

    implicit def system = DockerClient.this.system

    implicit def dispatcher = system.dispatcher
  }

  import system.dispatcher

  def run(containerConfig: ContainerConfig, hostConfig: HostConfig): Future[ContainerId] = {
    runLocal(containerConfig, hostConfig).recoverWith {
      case _: UnknownResponseException =>
        images.create(containerConfig.image).flatMap { r =>
          val error = r.collectFirst { case e: Error => e}
          error match {
            case Some(_) =>
              throw new CreateImageException(containerConfig.image)
            case None =>
              runLocal(containerConfig, hostConfig)
          }
        }
    }
  }

  def runLocal(containerConfig: ContainerConfig, hostConfig: HostConfig): Future[ContainerId] = {
    containers.create(containerConfig).flatMap { response =>
      containers.start(response.id, hostConfig)
    }
  }
}

trait DockerCommands extends DockerPipeline {
  implicit def system: ActorSystem

  implicit def dispatcher: ExecutionContext
}

trait HostCommands extends DockerCommands {
  def ping = getRequest[String](Path / "_ping").map(_ == "OK")
}

trait ImageCommands extends DockerCommands {
  def list = getRequest[Seq[Image]](Path / "images" / "json")

  def create(imageName: ImageName): Future[Seq[Progress]] = {
    val parameters = Map(
      "fromImage" -> Option(imageName.repository),
      "tag" -> imageName.tag,
      "registry" -> imageName.registry,
      "repo" -> imageName.namespace)

    val uri = baseUri
      .withPath(Path / "images" / "create")
      .withQuery(parameters.mapValues(_.getOrElse("")))

    requestChunkedLines(HttpRequest(POST, uri)) recover {
      case ex: UnsuccessfulResponseException if ex.response.status == StatusCodes.NotFound =>
        throw new CreateImageException(imageName)
    } map { lines =>
      lines.map { line =>
        val obj = read[JObject](line)
        obj.extractOpt[ProgressStatus]
          .orElse(obj.extractOpt[ImageStatus])
          .orElse(obj.extractOpt[Status])
          .orElse(obj.extractOpt[Error])
      }.flatten
    }
  }
}

trait ContainerCommands extends DockerCommands {
  def list(all: Boolean = false) = getRequest[Seq[ContainerStatus]](Path / "containers" / "json")

  def get(id: ContainerId) = getRequest[ContainerInfo](Path / "containers" / id.hash / "json")

  def create(config: ContainerConfig) = {
    postRequest[ContainerConfig, CreateContainerResponse](Path / "containers" / "create", content = config)
  }

  def start(id: ContainerId, config: HostConfig) = {
    val uri = baseUri.withPath(Path / "containers" / id.hash / "start")
    request(HttpRequest(POST, uri)) map containerResponse(id)
  }

  def stop(id: ContainerId, maxWaitTime: Option[Duration] = None) = {
    val uri = baseUri
      .withPath(Path / "containers" / id.hash / "stop")
      .withQuery("t" -> maxWaitTime.fold(Query.EmptyValue)(_.toSeconds.toString))
    request(HttpRequest(POST, uri)) map containerResponse(id)
  }

  def delete(id: ContainerId, removeVolumes: Option[Boolean] = None, force: Option[Boolean] = None) = {
    val uri = baseUri
      .withPath(Path / "containers" / id.hash)
      .withQuery(
        "t" -> removeVolumes.fold(Query.EmptyValue)(_.toString),
        "force" -> force.fold(Query.EmptyValue)(_.toString))
    request(HttpRequest(DELETE, uri)) map containerResponse(id)
  }

  private def containerResponse(id: ContainerId)(response: HttpResponse) = {
    response.status match {
      case StatusCodes.NoContent =>
        id
      case StatusCodes.NotFound =>
        throw new ContainerNotFoundException(id)
      case code =>
        throw new UnknownResponseException(code)
    }
  }
}
