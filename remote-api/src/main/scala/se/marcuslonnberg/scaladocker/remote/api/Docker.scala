package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorSystem
import akka.http.model.HttpMethods._
import akka.http.model.Uri._
import akka.http.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.Flow
import org.json4s.JObject
import org.json4s.native.Serialization._
import se.marcuslonnberg.scaladocker.remote.models._

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

case class DockerClient(hostname: String, port: Int = 2375)(implicit system: ActorSystem, materializer: FlowMaterializer) {
  val containers = new ContainerCommands with Context
  val host = new HostCommands with Context
  val images = new ImageCommands with Context

  trait Context {
    this: DockerCommands =>
    override def baseUri = Uri(s"http://$hostname:$port")

    implicit def system = DockerClient.this.system

    implicit def materializer = DockerClient.this.materializer

    implicit def dispatcher = system.dispatcher
  }

  import system.dispatcher

  def run(containerConfig: ContainerConfig, hostConfig: HostConfig)(implicit materializer: FlowMaterializer): Future[ContainerId] = {
    runLocal(containerConfig, hostConfig).recoverWith {
      case _: UnknownResponseException =>
        images.create(containerConfig.image).flatMap { r =>
          val eventualError = r.collect { case e: Error => e}.toFuture
          eventualError.map {
            case error =>
              throw new CreateImageException(containerConfig.image)
          }.recoverWith {
            case e: NoSuchElementException =>
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

  implicit def materializer: FlowMaterializer

  implicit def dispatcher: ExecutionContext
}

trait HostCommands extends DockerCommands {
  def ping = getRequest[String](Path / "_ping").map(_ == "OK")
}

trait ImageCommands extends DockerCommands {
  def list = getRequest[Seq[Image]](Path / "images" / "json")

  def create(imageName: ImageName): Future[Flow[Progress]] = {
    val parameters = Map(
      "fromImage" -> Option(imageName.repository),
      "tag" -> Option(imageName.tag),
      "registry" -> imageName.registry,
      "repo" -> imageName.namespace)

    val uri = baseUri
      .withPath(Path / "images" / "create")
      .withQuery(parameters.mapValues(_.getOrElse("")))

    requestChunkedLines(HttpRequest(POST, uri)).map { lines =>
      lines
        .filter(_.nonEmpty)
        .map { line =>
        val obj = read[JObject](line)
        obj.extractOpt[ProgressStatus]
          .orElse(obj.extractOpt[ImageStatus])
          .orElse(obj.extractOpt[Status])
          .orElse(obj.extractOpt[Error])
      }.collect {
        case Some(v) => v
      }
    }
  }
}

trait ContainerCommands extends DockerCommands {
  def list(all: Boolean = false) = getRequest[Seq[ContainerStatus]](Path / "containers" / "json", Query("all" -> all.toString))

  def get(id: ContainerId) = getRequest[ContainerInfo](Path / "containers" / id.hash / "json")

  def create(config: ContainerConfig) = {
    postRequest[ContainerConfig, CreateContainerResponse](Path / "containers" / "create", content = config)
  }

  def start(id: ContainerId, config: HostConfig) = {
    val uri = baseUri.withPath(Path / "containers" / id.hash / "start")
    sendRequest(HttpRequest(POST, uri)) map containerResponse(id)
  }

  def stop(id: ContainerId, maxWaitTime: Option[Duration] = None) = {
    val uri = baseUri
      .withPath(Path / "containers" / id.hash / "stop")
      .withQuery("t" -> maxWaitTime.fold(Query.EmptyValue)(_.toSeconds.toString))
    sendRequest(HttpRequest(POST, uri)) map containerResponse(id)
  }

  def delete(id: ContainerId, removeVolumes: Option[Boolean] = None, force: Option[Boolean] = None) = {
    val uri = baseUri
      .withPath(Path / "containers" / id.hash)
      .withQuery(
        "t" -> removeVolumes.fold(Query.EmptyValue)(_.toString),
        "force" -> force.fold(Query.EmptyValue)(_.toString))
    sendRequest(HttpRequest(DELETE, uri)) map containerResponse(id)
  }

  def logs(id: ContainerId, follow: Boolean = false, stdout: Boolean = true, stderr: Boolean = false, timestamps: Boolean = false): Future[Flow[String]] = {
    val uri = baseUri
      .withPath(Path / "containers" / id.hash / "logs")
      .withQuery(
        "follow" -> follow.toString,
        "stdout" -> stdout.toString,
        "stderr" -> stderr.toString,
        "timestamps" -> timestamps.toString)
    requestChunkedLines(HttpRequest(GET, uri))
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
