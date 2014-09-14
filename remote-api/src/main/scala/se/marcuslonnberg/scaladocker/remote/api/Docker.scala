package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorSystem
import akka.http.model.HttpMethods._
import akka.http.model.Uri._
import akka.http.model._
import akka.stream.scaladsl2._
import org.json4s.JObject
import org.json4s.native.Serialization._
import org.reactivestreams.Publisher
import se.marcuslonnberg.scaladocker.remote.models._

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

object DockerClient {
  def apply(host: String, port: Int = 2375)(implicit system: ActorSystem, materializer: FlowMaterializer): DockerClient =
    apply(Uri(s"http://$host:$port"))
}

case class DockerClient(baseUri: Uri)(implicit system: ActorSystem, materializer: FlowMaterializer) {
  val containers = new ContainerCommands with Context
  val host = new HostCommands with Context
  val images = new ImageCommands with BuildCommand with Context

  trait Context {
    this: DockerCommands =>
    override def baseUri = DockerClient.this.baseUri

    implicit def system = DockerClient.this.system

    implicit def materializer = DockerClient.this.materializer

    implicit def dispatcher = system.dispatcher
  }

  import system.dispatcher

  def run(containerConfig: ContainerConfig, hostConfig: HostConfig)(implicit materializer: FlowMaterializer): Future[ContainerId] = {
    runLocal(containerConfig, hostConfig).recoverWith {
      case _: UnknownResponseException =>
        val create = images.create(containerConfig.image)

        val eventualErrorSink = FutureSink[Error]
        val mat = FlowFrom(create).collect { case e: Error => e}.withSink(eventualErrorSink).run()

        eventualErrorSink.future(mat).map {
          case error =>
            throw new CreateImageException(containerConfig.image)
        }.recoverWith {
          case e: NoSuchElementException =>
            runLocal(containerConfig, hostConfig)
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

  def create(imageName: ImageName): Publisher[CreateMessage] = {
    val parameters = Map(
      "fromImage" -> Option(imageName.repository),
      "tag" -> Option(imageName.tag),
      "registry" -> imageName.registry,
      "repo" -> imageName.namespace)

    val uri = baseUri
      .withPath(Path / "images" / "create")
      .withQuery(parameters.mapValues(_.getOrElse("")))

    FlowFrom(requestChunkedLines(HttpRequest(POST, uri)))
      .filter(_.nonEmpty)
      .map { line =>
      val obj = read[JObject](line)
      val out: Option[CreateMessage] = obj.extractOpt[CreateMessages.Progress]
        .orElse(obj.extractOpt[CreateMessages.ImageStatus])
        .orElse(obj.extractOpt[CreateMessages.Status])
        .orElse(obj.extractOpt[CreateMessages.Error])
      out
    }.collect {
      case Some(v) => v
    }.toPublisher()
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

  def logs(id: ContainerId, follow: Boolean = false, stdout: Boolean = true, stderr: Boolean = false, timestamps: Boolean = false): Publisher[String] = {
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
