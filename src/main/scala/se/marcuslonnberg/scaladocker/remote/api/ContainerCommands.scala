package se.marcuslonnberg.scaladocker.remote.api

import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{FlattenStrategy, Flow, Source}
import org.joda.time.{DateTime, Seconds}
import se.marcuslonnberg.scaladocker.remote.api.PlayJsonSupport._
import se.marcuslonnberg.scaladocker.remote.models._
import se.marcuslonnberg.scaladocker.remote.models.json._

import scala.concurrent.{ExecutionContext, Future}

class ContainerCommands(connection: DockerConnection) extends Commands {

  import connection._

  protected val containersPath = Path / "containers"

  def list(
    all: Boolean
  )(implicit ec: ExecutionContext): Future[Seq[ContainerStatus]] = {
    val request = Get(buildUri(containersPath / "json", Query("all" -> all.toString)))
    connection.sendRequest(request).flatMap { response =>
      response.status match {
        case StatusCodes.OK =>
          Unmarshal(response).to[Seq[ContainerStatus]]
        case _ =>
          unknownResponseFuture(response)
      }
    }
  }

  def get(
    containerId: ContainerId
  )(implicit ec: ExecutionContext): Future[ContainerInfo] = {
    val request = Get(buildUri(containersPath / containerId.value / "json"))
    connection.sendRequest(request).flatMap { response =>
      response.status match {
        case StatusCodes.OK =>
          Unmarshal(response).to[ContainerInfo]
        case StatusCodes.NotFound =>
          throw new ContainerNotFoundException(containerId)
        case _ =>
          unknownResponseFuture(response)
      }
    }
  }

  def create(
    containerConfig: ContainerConfig,
    hostConfig: HostConfig,
    containerName: Option[ContainerName]
  )(implicit ec: ExecutionContext): Future[CreateContainerResponse] = {
    val configJson = containerConfigFormat.writes(containerConfig) + ("HostConfig" -> hostConfigFormat.writes(hostConfig))
    val query = containerName.map(name => Query("name" -> name.value)).getOrElse(Query())
    val request = Post(buildUri(containersPath / "create", query), configJson)
    connection.sendRequest(request).flatMap { response =>
      response.status match {
        case StatusCodes.Created =>
          Unmarshal(response).to[CreateContainerResponse]
        case _ =>
          unknownResponseFuture(response)
      }
    }
  }

  def start(
    containerId: ContainerId,
    hostConfig: Option[HostConfig] = None
  )(implicit ec: ExecutionContext): Future[ContainerId] = {
    val request = Post(buildUri(containersPath / containerId.value / "start"), hostConfig)
    connection.sendRequest(request).flatMap { response =>
      response.status match {
        case StatusCodes.NoContent =>
          Future.successful(containerId)
        case StatusCodes.NotFound =>
          throw new ContainerNotFoundException(containerId)
        case _ =>
          unknownResponseFuture(response)
      }
    }
  }

  def stop(
    containerId: ContainerId,
    maximumWait: Seconds
  )(implicit ec: ExecutionContext): Future[ContainerId] = {
    val request = Post(buildUri(containersPath / containerId.value / "stop", Query("t" -> maximumWait.getSeconds.toString)))
    connection.sendRequest(request).flatMap { response =>
      response.status match {
        case StatusCodes.NoContent =>
          Future.successful(containerId)
        case StatusCodes.NotModified =>
          throw new ContainerAlreadyStoppedException(containerId)
        case StatusCodes.NotFound =>
          throw new ContainerNotFoundException(containerId)
        case _ =>
          unknownResponseFuture(response)
      }
    }
  }

  def restart(
    containerId: ContainerId,
    maximumWait: Seconds
  )(implicit ec: ExecutionContext): Future[ContainerId] = {
    val request = Post(buildUri(containersPath / containerId.value / "stop", Query("t" -> maximumWait.getSeconds.toString)))
    connection.sendRequest(request).flatMap { response =>
      response.status match {
        case StatusCodes.NoContent =>
          Future.successful(containerId)
        case StatusCodes.NotFound =>
          throw new ContainerNotFoundException(containerId)
        case _ =>
          unknownResponseFuture(response)
      }
    }
  }

  def remove(
    containerId: ContainerId,
    force: Boolean,
    removeVolumes: Boolean
  )(implicit ec: ExecutionContext): Future[ContainerId] = {
    val query = Query("force" -> force.toString, "v" -> removeVolumes.toString)
    val request = Delete(buildUri(containersPath / containerId.value, query))
    connection.sendRequest(request).flatMap { response =>
      response.status match {
        case StatusCodes.NoContent =>
          Future.successful(containerId)
        case StatusCodes.NotFound =>
          throw new ContainerNotFoundException(containerId)
        case _ =>
          unknownResponseFuture(response)
      }
    }
  }

  def logs(
    containerId: ContainerId,
    follow: Boolean,
    stdout: Boolean,
    stderr: Boolean,
    since: Option[DateTime],
    timestamps: Boolean,
    tailLimit: Option[Int]
  )(implicit ec: ExecutionContext): Source[String, Unit] = {
    val query = Query(
      "follow" -> follow.toString,
      "stdout" -> stdout.toString,
      "stderr" -> stderr.toString,
      "since" -> since.map(_.getMillis).getOrElse(0).toString,
      "timestamps" -> timestamps.toString,
      "tail" -> tailLimit.map(_.toString).getOrElse("all")
    )
    val request = Get(buildUri(containersPath / containerId.value / "logs", query))
    val contentType = ContentType(DockerApi.MediaTypes.`application/vnd.docker.raw-stream`)

    val flow: Flow[HttpResponse, String, Unit] =
      Flow[HttpResponse].map {
        case HttpResponse(StatusCodes.OK, _, HttpEntity.Chunked(_, chunks), _) =>
          chunks.map(_.data().utf8String)
        case HttpResponse(StatusCodes.NotFound, _, _, _) =>
          throw new ContainerNotFoundException(containerId)
        case response =>
          unknownResponse(response)
      }.flatten(FlattenStrategy.concat)

    Source(connection.sendRequest(request))
      .via(flow)
  }
}
