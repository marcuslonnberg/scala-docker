package se.marcuslonnberg.scaladocker.remote.api

import spray.http.HttpMethods.{DELETE, POST}
import spray.http._
import spray.http.Uri._
import akka.actor.ActorRefFactory
import scala.concurrent.duration.Duration
import spray.http.HttpRequest
import se.marcuslonnberg.scaladocker.remote.models._

case class DockerHost(host: String, port: Int = 4243)(implicit val actorRefFactory: ActorRefFactory) extends DockerHostCommands with DockerPipeline {
  override def baseUri = Uri(s"http://$host:$port")
}

trait DockerHostCommands {
  this: DockerPipeline =>

  implicit def actorRefFactory: ActorRefFactory

  private implicit val dispatcher = actorRefFactory.dispatcher

  def ping = get[String](Path / "_ping").map(_ == "OK")

  def images = get[Seq[Image]](Path / "images" / "json")

  def containers(all: Boolean = false) = get[Seq[ContainerStatus]](Path / "containers" / "json")

  def container(id: ContainerId) = get[ContainerInfo](Path / "containers" / id.id / "json")

  def createContainer(config: ContainerConfig) = {
    post[ContainerConfig, CreateContainerResponse](Path / "containers" / "create", content = config)
  }

  def startContainer(id: ContainerId, config: HostConfig) = {
    val uri = baseUri.withPath(Path / "containers" / id.id / "start")
    request(HttpRequest(POST, uri)) map containerResponse(id)
  }

  def stopContainer(id: ContainerId, maxWaitTime: Option[Duration] = None) = {
    val uri = baseUri
      .withPath(Path / "containers" / id.id / "stop")
      .withQuery("t" -> maxWaitTime.fold(Query.EmptyValue)(_.toSeconds.toString))
    request(HttpRequest(POST, uri)) map containerResponse(id)
  }

  def deleteContainer(id: ContainerId, removeVolumes: Option[Boolean] = None, force: Option[Boolean] = None) = {
    val uri = baseUri
      .withPath(Path / "containers" / id.id)
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
