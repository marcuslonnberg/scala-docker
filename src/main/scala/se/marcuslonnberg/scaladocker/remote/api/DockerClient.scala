package se.marcuslonnberg.scaladocker.remote.api

import java.io.File

import akka.stream.scaladsl.{FlattenStrategy, Sink, Source}
import akka.util.ByteString
import org.joda.time.{DateTime, Seconds}
import se.marcuslonnberg.scaladocker.remote.models._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

class DockerClient(connection: DockerConnection) {

  import connection._

  val container = new ContainerCommands(connection)

  val image = new ImageCommands(connection)

  val host = new HostCommands(connection)

  def ps(all: Boolean = false)(implicit ec: ExecutionContext): Future[Seq[ContainerStatus]] = listContainers(all)

  def listContainers(all: Boolean = false)(implicit ec: ExecutionContext): Future[Seq[ContainerStatus]] = container.list(all)

  def images(all: Boolean = false)(implicit ec: ExecutionContext): Future[Seq[Image]] = listImages(all)

  def listImages(all: Boolean = false)(implicit ec: ExecutionContext): Future[Seq[Image]] = image.list(all)

  def inspect(containerId: ContainerId)(implicit ec: ExecutionContext): Future[ContainerInfo] = container.get(containerId)

  def create(
    containerConfig: ContainerConfig,
    hostConfig: HostConfig = HostConfig(),
    containerName: Option[ContainerName] = None
  )(implicit ec: ExecutionContext): Future[CreateContainerResponse] = {
    container.create(containerConfig, hostConfig, containerName)
  }

  def start(
    containerId: ContainerId,
    hostConfig: Option[HostConfig] = None
  )(implicit ec: ExecutionContext): Future[ContainerId] = {
    container.start(containerId, hostConfig)
  }

  def stop(
    containerId: ContainerId,
    maximumWait: Seconds = Seconds.seconds(10)
  )(implicit ec: ExecutionContext): Future[ContainerId] = {
    container.stop(containerId, maximumWait)
  }

  def restart(
    containerId: ContainerId,
    maximumWait: Seconds = Seconds.seconds(10)
  )(implicit ec: ExecutionContext): Future[ContainerId] = {
    container.restart(containerId, maximumWait)
  }

  def rm(
    containerId: ContainerId,
    force: Boolean = false,
    volumes: Boolean = false
  )(implicit ec: ExecutionContext): Future[ContainerId] = {
    removeContainer(containerId, force, volumes)
  }

  def rmi(
    imageId: ImageIdentifier,
    force: Boolean = false,
    prune: Boolean = true
  )(implicit ec: ExecutionContext): Source[RemoveImageMessage, Any] = {
    removeImage(imageId, force, prune)
  }

  def removeImage(
    imageId: ImageIdentifier,
    force: Boolean = false,
    prune: Boolean = true
  )(implicit ec: ExecutionContext): Source[RemoveImageMessage, Any] = {
    image.remove(imageId, force, prune)
  }

  def removeContainer(
    containerId: ContainerId,
    force: Boolean = false,
    volumes: Boolean = false
  )(implicit ec: ExecutionContext): Future[ContainerId] = {
    container.remove(containerId, force, volumes)
  }

  def logs(
    containerId: ContainerId,
    follow: Boolean = false,
    stdout: Boolean = true,
    stderr: Boolean = false,
    since: Option[DateTime] = None,
    timestamps: Boolean = false,
    tailLimit: Option[Int] = None
  )(implicit ec: ExecutionContext) = {
    container.logs(containerId, follow, stdout, stderr, since, timestamps, tailLimit)
  }

  def runLocal(
    containerConfig: ContainerConfig,
    hostConfig: HostConfig = HostConfig(),
    name: Option[ContainerName] = None
  )(implicit ec: ExecutionContext): Future[ContainerId] = {
    create(containerConfig, hostConfig, name).flatMap { response =>
      container.start(response.id)
    }
  }

  def run(
    containerConfig: ContainerConfig,
    hostConfig: HostConfig = HostConfig(),
    name: Option[ContainerName] = None
  )(implicit ec: ExecutionContext): Source[RunMessage, Unit] = {
    def runContainer = Source(runLocal(containerConfig, hostConfig, name)).map(RunMessage.ContainerStarted)
    runContainer.map(message => Source.single(message)).recover {
      case _: ImageNotFoundException =>
        image.create(containerConfig.image).map(RunMessage.CreatingImage) ++
          runContainer
    }.flatten(FlattenStrategy.concat)
  }

  def runFuture(
    containerConfig: ContainerConfig,
    hostConfig: HostConfig = HostConfig(),
    name: Option[ContainerName] = None
  )(implicit ec: ExecutionContext): Future[ContainerId] = {
    run(containerConfig, hostConfig, name).collect {
      case RunMessage.ContainerStarted(id) => id
    }.runWith(Sink.head)
  }

  def pull(imageName: ImageName)(implicit ec: ExecutionContext): Source[ImageTransferMessage, Unit] = {
    image.create(imageName)
  }

  def pullFuture(imageName: ImageName)(implicit ec: ExecutionContext): Future[ImageName] = {
    transferImageSourceToFuture(image.create(imageName), imageName)
  }

  def transferImageSourceToFuture(source: Source[ImageTransferMessage, Unit], imageName: ImageName): Future[ImageName] = {
    val promise = Promise[ImageName]()

    source.collect {
      case ImageTransferMessage.Error(error) =>
        throw new DockerPullImageException(error)
    }.runWith(Sink.onComplete {
      case Success(_) =>
        promise.success(imageName)
      case Failure(ex) =>
        promise.failure(ex)
    })

    promise.future
  }

  def push(imageName: ImageName)(implicit ec: ExecutionContext): Source[ImageTransferMessage, Unit] = image.push(imageName)

  def pushFuture(imageName: ImageName)(implicit ec: ExecutionContext): Future[ImageName] = {
    transferImageSourceToFuture(image.push(imageName), imageName)
  }

  def tag(
    imageId: ImageIdentifier,
    tag: ImageName,
    force: Boolean = false
  )(implicit ec: ExecutionContext): Future[ImageName] = image.tag(imageId, tag, force)

  def buildFile(
    tarFile: File,
    imageName: Option[ImageName] = None,
    cache: Boolean = true,
    rm: Boolean = true,
    alwaysPull: Boolean = false
  )(implicit ec: ExecutionContext): Source[BuildMessage, Unit] = image.buildFile(tarFile, imageName, cache, rm, alwaysPull)

  def build(
    tarStream: Source[ByteString, Any],
    tarLength: Long,
    imageName: Option[ImageName] = None,
    cache: Boolean = true,
    rm: Boolean = true,
    alwaysPull: Boolean = false
  )(implicit ec: ExecutionContext): Source[BuildMessage, Unit] = {
    image.build(tarStream, tarLength, imageName, cache, rm, alwaysPull)
  }
}
