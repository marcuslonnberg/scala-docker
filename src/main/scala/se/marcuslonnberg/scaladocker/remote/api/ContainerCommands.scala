package se.marcuslonnberg.scaladocker.remote.api

import akka.http.model.HttpMethods._
import akka.http.model.Uri.{Path, Query}
import akka.http.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.unmarshalling._
import org.reactivestreams.Publisher
import se.marcuslonnberg.scaladocker.remote.models._
import se.marcuslonnberg.scaladocker.remote.models.json._

import scala.concurrent.Future
import scala.concurrent.duration.Duration

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
        case StatusCodes.InternalServerError =>
          entityAsString(response).map { entity =>
            throw ServerErrorException(response.status, entity)
          }
        case _ =>
          unknownResponse(response)
      }
    }
  }

  def start(id: ContainerId, config: HostConfig = HostConfig()): Future[ContainerId] = {
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
