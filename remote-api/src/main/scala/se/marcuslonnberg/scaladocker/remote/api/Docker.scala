package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorSystem
import akka.http.model.HttpMethods._
import akka.http.model.Uri._
import akka.http.model._
import akka.http.unmarshalling.FromResponseUnmarshaller
import akka.stream.scaladsl2._
import org.json4s.JObject
import org.json4s.native.Serialization._
import org.reactivestreams.Publisher
import se.marcuslonnberg.scaladocker.remote.models._

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

object DockerClient {
  def apply()(implicit system: ActorSystem, materializer: FlowMaterializer): DockerClient = {
    val key = "DOCKER_HOST"
    val value = sys.env.get(key).filter(_.nonEmpty).getOrElse {
      sys.error(s"Environment variable $key is not set")
    }
    apply(Uri(value))
  }

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

    implicit def system: ActorSystem = DockerClient.this.system

    implicit def materializer: FlowMaterializer = DockerClient.this.materializer

    implicit def dispatcher: ExecutionContextExecutor = system.dispatcher
  }

  import system.dispatcher

  def run(containerConfig: ContainerConfig, hostConfig: HostConfig)(implicit materializer: FlowMaterializer): Future[ContainerId] = {
    runLocal(containerConfig, hostConfig).recoverWith {
      case _: ImageNotFoundException =>
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
  def ping(): Future[Boolean] = {
    sendGetRequest(Path / "_ping").map { response =>
      response.status match {
        case StatusCodes.OK =>
          true
        case _ =>
          false
      }
    }.recover {
      case _ => false
    }
  }
}

trait ImageCommands extends DockerCommands {
  def list(): Future[Seq[Image]] = {
    sendGetRequest(Path / "images" / "json").flatMap { response =>
      response.status match {
        case StatusCodes.OK =>
          val unmarshaller = implicitly[FromResponseUnmarshaller[Seq[Image]]]
          unmarshaller(response)
        case StatusCodes.InternalServerError =>
          Future.failed(ServerErrorException(response.status, "")) // TODO: Use entity as message
        case status =>
          Future.failed(UnknownResponseException(status))
      }
    }
  }

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

  def tag(from: ImageName, to: ImageName, force: Boolean = false): Future[Boolean] = {
    val parameters = Map(
      "repo" -> (to.registry.fold("")(_ + "/") + to.namespace.fold("")(_ + "/") + to.repository),
      "tag" -> to.tag,
      "force" -> force.toString)

    val uri = baseUri
      .withPath(Path / "images" / from.toString / "tag")
      .withQuery(parameters)

    sendRequest(HttpRequest(POST, uri))
      .map(_.status == StatusCodes.Created)
  }

  def delete(name: ImageName, force: Boolean = false, noPrune: Boolean = false): Future[Boolean] = {
    val parameters = Map(
      "force" -> force.toString,
      "noprune" -> noPrune.toString)

    val uri = baseUri
      .withPath(Path / "images" / name.toString)
      .withQuery(parameters)

    sendRequest(HttpRequest(DELETE, uri))
      .map(_.status == StatusCodes.OK)
  }
}

trait ContainerCommands extends DockerCommands {
  def list(all: Boolean = false): Future[Seq[ContainerStatus]] = {
    sendGetRequest(Path / "containers" / "json", Query("all" -> all.toString)).flatMap { response =>
      response.status match {
        case StatusCodes.OK =>
          val unmarshaller = implicitly[FromResponseUnmarshaller[Seq[ContainerStatus]]]
          unmarshaller(response)
        case StatusCodes.BadRequest =>
          Future.failed(BadRequestException("")) // TODO: Use entity as message
        case StatusCodes.InternalServerError =>
          Future.failed(ServerErrorException(response.status, "")) // TODO: Use entity as message
        case status =>
          Future.failed(UnknownResponseException(status))
      }
    }
  }

  def get(id: ContainerId): Future[ContainerInfo] = {
    sendGetRequest(Path / "containers" / id.hash / "json").flatMap { response =>
      response.status match {
        case StatusCodes.OK =>
          val unmarshaller = implicitly[FromResponseUnmarshaller[ContainerInfo]]
          unmarshaller(response)
        case StatusCodes.NotFound =>
          Future.failed(ContainerNotFoundException(id))
        case StatusCodes.InternalServerError =>
          Future.failed(ServerErrorException(response.status, "")) // TODO: Use entity as message
        case status =>
          Future.failed(UnknownResponseException(status))
      }
    }
  }

  def create(config: ContainerConfig): Future[CreateContainerResponse] = {
    sendPostRequest(Path / "containers" / "create", content = config).flatMap { response =>
      response.status match {
        case StatusCodes.Created =>
          val unmarshaller = implicitly[FromResponseUnmarshaller[CreateContainerResponse]]
          unmarshaller(response)
        case StatusCodes.NotFound =>
          Future.failed(ImageNotFoundException(config.image.toString))
        case status =>
          Future.failed(UnknownResponseException(status))
      }
    }
  }

  def start(id: ContainerId, config: HostConfig) = {
    sendPostRequest(Path / "containers" / id.hash / "start", content = config).map(containerResponse(id))
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
