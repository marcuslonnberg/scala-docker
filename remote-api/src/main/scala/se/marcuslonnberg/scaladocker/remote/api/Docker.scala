package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorRefFactory
import se.marcuslonnberg.scaladocker.remote.models._
import spray.http.HttpMethods.{DELETE, POST}
import spray.http.Uri._
import spray.http._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

case class DockerClient(hostname: String, port: Int = 4243)(implicit val actorRefFactory: ActorRefFactory) {
  val containers = new ContainerCommands with Context
  val host = new HostCommands with Context
  val images = new ImageCommands with Context

  trait Context {
    this: DockerCommands =>
    override def baseUri = Uri(s"http://$hostname:$port")

    implicit def actorRefFactory = DockerClient.this.actorRefFactory

    implicit def dispatcher = actorRefFactory.dispatcher
  }

}

trait DockerCommands extends DockerPipeline {
  implicit def actorRefFactory: ActorRefFactory

  implicit def dispatcher: ExecutionContext
}

trait HostCommands extends DockerCommands {
  def ping = getRequest[String](Path / "_ping").map(_ == "OK")
}

trait ImageCommands extends DockerCommands {
  def list = getRequest[Seq[Image]](Path / "images" / "json")
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
