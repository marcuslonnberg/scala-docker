package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorSystem
import akka.http.model.HttpMethods._
import akka.http.model.Uri._
import akka.http.model._
import akka.http.model.headers.RawHeader
import akka.http.unmarshalling.{FromResponseUnmarshaller, PredefinedFromEntityUnmarshallers}
import akka.stream._
import akka.stream.scaladsl._
import org.apache.commons.codec.binary.Base64
import org.reactivestreams.Publisher
import play.api.libs.json.Json
import se.marcuslonnberg.scaladocker.remote.models._
import se.marcuslonnberg.scaladocker.remote.models.json._

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

object DockerClient {
  def apply()(implicit system: ActorSystem, materializer: FlowMaterializer): DockerClient = {
    val key = "DOCKER_HOST"
    val value = sys.env.get(key).filter(_.nonEmpty).getOrElse {
      sys.error(s"Environment variable $key is not set")
    }
    apply(Uri(value.replaceFirst("tcp:", "http:")), Seq.empty)
  }

  def apply(host: String, port: Int = 2375)(implicit system: ActorSystem, materializer: FlowMaterializer): DockerClient =
    apply(Uri(s"http://$host:$port"), Seq.empty)
}

case class DockerClient(baseUri: Uri, auths: Seq[RegistryAuth])(implicit system: ActorSystem, materializer: FlowMaterializer) {
  val containers: ContainerCommands = new ContainerCommands with Context
  val host: HostCommands = new HostCommands with Context
  val images: ImageCommands = new ImageCommands with BuildCommand with Context {
    override private[api] def auths = DockerClient.this.auths
  }

  trait Context {
    this: DockerCommands =>
    private[api] override def baseUri = DockerClient.this.baseUri

    private[api] implicit def system: ActorSystem = DockerClient.this.system

    private[api] implicit def materializer: FlowMaterializer = DockerClient.this.materializer

    private[api] implicit def dispatcher: ExecutionContextExecutor = system.dispatcher
  }

  import system.dispatcher

  def run(containerConfig: ContainerConfig,
    hostConfig: HostConfig,
    name: Option[ContainerName] = None
  )(implicit materializer: FlowMaterializer): Future[ContainerId] = {
    runLocal(containerConfig, hostConfig, name).recoverWith {
      case _: ImageNotFoundException =>
        val create = images.create(containerConfig.image)

        val eventualError = Source(create).collect { case e: CreateMessages.Error => e }.runWith(Sink.head[CreateMessages.Error])
        eventualError.map {
          case error =>
            throw new CreateImageException(containerConfig.image)
        }.recoverWith {
          case e: NoSuchElementException =>
            runLocal(containerConfig, hostConfig)
        }
    }
  }

  def runLocal(
    containerConfig: ContainerConfig,
    hostConfig: HostConfig,
    name: Option[ContainerName] = None
  ): Future[ContainerId] = {
    containers.create(containerConfig, name).flatMap { response =>
      containers.start(response.id, hostConfig)
    }
  }

  def withAuths(auths: Seq[RegistryAuth]) = copy(auths = auths)
}

trait DockerCommands extends DockerPipeline {
  private[api] implicit def system: ActorSystem

  private[api] implicit def materializer: FlowMaterializer

  private[api] implicit def dispatcher: ExecutionContext

  private[api] def entityAsString(response: HttpResponse): Future[String] = {
    val unmarshaller = PredefinedFromEntityUnmarshallers.stringUnmarshaller
    unmarshaller(response.entity)
  }

  private[api] def unknownResponse(response: HttpResponse): Future[Nothing] = {
    entityAsString(response).map { entity =>
      throw UnknownResponseException(response.status, entity)
    }
  }
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

trait ImageCommands extends DockerCommands with AuthUtils {
  def list(): Future[Seq[Image]] = {
    sendGetRequest(Path / "images" / "json").flatMap { response =>
      response.status match {
        case StatusCodes.OK =>
          val unmarshaller = implicitly[FromResponseUnmarshaller[Seq[Image]]]
          unmarshaller(response)
        case StatusCodes.InternalServerError =>
          entityAsString(response).map { entity =>
            throw ServerErrorException(response.status, entity)
          }
        case _ =>
          unknownResponse(response)
      }
    }
  }

  def create(imageName: ImageName): Publisher[CreateMessage] = {
    val parameters = Map("fromImage" -> imageName.toString)

    val uri = baseUri
      .withPath(Path / "images" / "create")
      .withQuery(parameters)

    val headers = getAuthHeader(imageName.registry).toList

    Source(requestChunkedLines(HttpRequest(POST, uri, headers)))
      .filter(_.nonEmpty)
      .map { line =>
      val out: Option[CreateMessage] = ???
      out
    }.collect {
      case Some(v) => v
    }.runWith(Sink.publisher[CreateMessage])
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
      .map { response =>
      response.status match {
        case StatusCodes.Created => true
        case _ => false
      }
    }
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
          entityAsString(response).map { entity =>
            throw BadRequestException(entity)
          }
        case StatusCodes.InternalServerError =>
          entityAsString(response).map { entity =>
            throw ServerErrorException(response.status, entity)
          }
        case _ =>
          unknownResponse(response)
      }
    }
  }

  def get(id: ContainerId): Future[ContainerInfo] = {
    sendGetRequest(Path / "containers" / id.value / "json").flatMap { response =>
      response.status match {
        case StatusCodes.OK =>
          val unmarshaller = implicitly[FromResponseUnmarshaller[ContainerInfo]]
          unmarshaller(response)
        case StatusCodes.NotFound =>
          Future.failed(ContainerNotFoundException(id))
        case StatusCodes.InternalServerError =>
          entityAsString(response).map { entity =>
            throw ServerErrorException(response.status, entity)
          }
        case _ =>
          unknownResponse(response)
      }
    }
  }

  def create(config: ContainerConfig, name: Option[ContainerName] = None): Future[CreateContainerResponse] = {
    val query = name.map(n => Uri.Query("name" -> n.value)).getOrElse(Uri.Query.Empty)
    sendPostRequest(Path / "containers" / "create", query, config).flatMap { response =>
      response.status match {
        case StatusCodes.Created =>
          val unmarshaller = implicitly[FromResponseUnmarshaller[CreateContainerResponse]]
          unmarshaller(response)
        case StatusCodes.NotFound =>
          Future.failed(ImageNotFoundException(config.image.toString))
        case _ =>
          unknownResponse(response)
      }
    }
  }

  def start(id: ContainerId, config: HostConfig): Future[ContainerId] = {
    sendPostRequest(Path / "containers" / id.value / "start", content = config).flatMap(containerResponse(id))
  }

  def stop(
    id: ContainerId,
    maxWaitTime: Option[Duration] = None
  ): Future[ContainerId] = {
    val uri = baseUri
      .withPath(Path / "containers" / id.value / "stop")
      .withQuery("t" -> maxWaitTime.fold(Query.EmptyValue)(_.toSeconds.toString))
    sendRequest(HttpRequest(POST, uri)).flatMap(containerResponse(id))
  }

  def delete(
    id: ContainerId,
    removeVolumes: Option[Boolean] = None,
    force: Option[Boolean] = None
  ): Future[ContainerId] = {
    val uri = baseUri
      .withPath(Path / "containers" / id.value)
      .withQuery(
        "t" -> removeVolumes.fold(Query.EmptyValue)(_.toString),
        "force" -> force.fold(Query.EmptyValue)(_.toString))
    sendRequest(HttpRequest(DELETE, uri)) flatMap containerResponse(id)
  }

  def logs(
    id: ContainerId,
    follow: Boolean = false,
    stdout: Boolean = true,
    stderr: Boolean = false,
    timestamps: Boolean = false
  ): Publisher[String] = {
    val uri = baseUri
      .withPath(Path / "containers" / id.value / "logs")
      .withQuery(
        "follow" -> follow.toString,
        "stdout" -> stdout.toString,
        "stderr" -> stderr.toString,
        "timestamps" -> timestamps.toString)
    requestChunkedLines(HttpRequest(GET, uri))
  }

  private def containerResponse(id: ContainerId)(response: HttpResponse): Future[ContainerId] = {
    response.status match {
      case StatusCodes.NoContent =>
        Future.successful(id)
      case StatusCodes.NotFound =>
        Future.failed(new ContainerNotFoundException(id))
      case _ =>
        unknownResponse(response)
    }
  }
}
