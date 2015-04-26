package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorSystem
import akka.http.model._
import akka.http.unmarshalling.PredefinedFromEntityUnmarshallers
import akka.stream._
import akka.stream.scaladsl._
import se.marcuslonnberg.scaladocker.remote.models._

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
  val images: ImageCommands with BuildCommand = new ImageCommands with BuildCommand with Context {
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

  def run(
    containerConfig: ContainerConfig,
    hostConfig: HostConfig = HostConfig(),
    name: Option[ContainerName] = None
  )(implicit materializer: FlowMaterializer): Future[ContainerId] = {
    runLocal(containerConfig, hostConfig, name).recoverWith {
      case _: ImageNotFoundException =>
        val create = images.create(containerConfig.image)

        val eventualError = Source(create).collect { case e: CreateImageMessages.Error => e }.runWith(Sink.head[CreateImageMessages.Error])
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
    hostConfig: HostConfig = HostConfig(),
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
