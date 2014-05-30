package se.marcuslonnberg.scaladocker.remote.api

import spray.http._
import spray.http.Uri._
import akka.actor.ActorRefFactory
import scala.concurrent.duration.Duration
import spray.http.HttpRequest
import se.marcuslonnberg.scaladocker.remote.models._

case class DockerHost(host: String, port: Int = 4243) extends DockerHostCommands with DockerPipeline {
  override def baseUri = Uri(scheme = "http", authority = Authority(Host(host), port))
}

trait DockerHostCommands {
  this: DockerPipeline =>

  def ping()(implicit actorRefFactory: ActorRefFactory) = {
    import actorRefFactory.dispatcher
    get[String](Path / "_ping").map(_ == "OK")
  }

  def images()(implicit actorRefFactory: ActorRefFactory) =
    get[Seq[Image]](Path / "images" / "json")

  def containers(all: Boolean = false)(implicit actorRefFactory: ActorRefFactory) =
    get[Seq[ContainerStatus]](Path / "containers" / "json")

  def createContainer(config: ContainerConfig)(implicit actorRefFactory: ActorRefFactory) =
    post[ContainerConfig, CreateContainerResponse](Path / "containers" / "create", content = config)

  def startContainer(id: ContainerId, config: HostConfig)(implicit actorRefFactory: ActorRefFactory) = {
    import actorRefFactory.dispatcher
    val uri = baseUri.withPath(Path / "containers" / id.id / "start")
    request(HttpRequest(method = HttpMethods.POST, uri = uri)) map containerResponse(id)
  }

  def stopContainer(id: ContainerId, maxWaitTime: Option[Duration] = None)(implicit actorRefFactory: ActorRefFactory) = {
    import actorRefFactory.dispatcher
    val uri = baseUri
      .withPath(Path / "containers" / id.id / "stop")
      .withQuery("t" -> maxWaitTime.fold(Query.EmptyValue)(_.toSeconds.toString))
    request(HttpRequest(method = HttpMethods.POST, uri = uri)) map containerResponse(id)
  }

  def deleteContainer(id: ContainerId, removeVolumes: Option[Boolean] = None, force: Option[Boolean] = None)(implicit actorRefFactory: ActorRefFactory) = {
    import actorRefFactory.dispatcher
    val uri = baseUri
      .withPath(Path / "containers" / id.id)
      .withQuery(
        "t" -> removeVolumes.fold(Query.EmptyValue)(_.toString),
        "force" -> force.fold(Query.EmptyValue)(_.toString))
    request(HttpRequest(method = HttpMethods.DELETE, uri = uri)) map containerResponse(id)
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
