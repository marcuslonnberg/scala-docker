package se.marcuslonnberg.scaladocker.remote.api

import java.io.File

import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.io.SynchronousFileSource
import akka.stream.scaladsl.{FlattenStrategy, Flow, Source}
import akka.util.ByteString
import play.api.libs.json.Json
import se.marcuslonnberg.scaladocker.remote.api.PlayJsonSupport._
import se.marcuslonnberg.scaladocker.remote.models._
import se.marcuslonnberg.scaladocker.remote.models.json._

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class ImageCommands(connection: DockerConnection) extends Commands {

  import connection._

  protected val imagesPath = Path / "images"

  def list(
    all: Boolean
  )(implicit ec: ExecutionContext): Future[Seq[Image]] = {
    val query = Query("all" -> all.toString)
    val request = Get(buildUri(imagesPath / "json", query))
    connection.sendRequest(request).flatMap { response =>
      response.status match {
        case StatusCodes.OK =>
          Unmarshal(response).to[Seq[Image]]
        case _ =>
          unknownResponseFuture(response)
      }
    }
  }

  def get(
    imageId: ImageIdentifier
  )(implicit ec: ExecutionContext): Future[Image] = {
    val request = Get(buildUri(imagesPath / imageId.toString / "json"))
    connection.sendRequest(request).flatMap { response =>
      response.status match {
        case StatusCodes.OK =>
          Unmarshal(response).to[Image]
        case StatusCodes.NotFound =>
          throw new ImageNotFoundException(imageId.toString)
        case _ =>
          unknownResponseFuture(response)
      }
    }
  }

  def create(
    imageName: ImageName
  )(implicit ec: ExecutionContext): Source[ImageTransferMessage, Unit] = {
    val authHeader = AuthUtils.getAuthHeader(connection.auths, imageName.registry.map(Uri(_)))

    val query = Query("fromImage" -> imageName.toString)
    val request = Post(buildUri(imagesPath / "create", query))
      .withHeaders(authHeader.to[scala.collection.immutable.Seq])

    val flow: Flow[HttpResponse, ImageTransferMessage, Unit] =
      Flow[HttpResponse].map {
        case HttpResponse(StatusCodes.OK, _, HttpEntity.Chunked(_, chunks), _) =>
          chunks.map(_.data().utf8String).filter(_.nonEmpty).map { str =>
            Json.parse(str).as[ImageTransferMessage]
          }
        case response =>
          unknownResponse(response)
      }.flatten(FlattenStrategy.concat)

    Source(connection.sendRequest(request))
      .via(flow)
  }

  def push(
    imageName: ImageName
  )(implicit ec: ExecutionContext): Source[ImageTransferMessage, Unit] = {
    val authHeader = AuthUtils.getAuthHeader(connection.auths, imageName.registry.map(Uri(_)))

    val imageNamePath = imagesPath ++ Path./ ++ Path(imageName.nameWithoutTag) ++ Path / "push"
    val request = Post(buildUri(imageNamePath, Query("tag" -> imageName.tag)))
      .withHeaders(authHeader.to[immutable.Seq])

    val flow: Flow[HttpResponse, ImageTransferMessage, Unit] =
      Flow[HttpResponse].map {
        case HttpResponse(StatusCodes.OK, _, HttpEntity.Chunked(_, chunks), _) =>
          chunks.map(_.data().utf8String).filter(_.nonEmpty).map { str =>
            Json.parse(str).as[ImageTransferMessage]
          }
        case HttpResponse(StatusCodes.NotFound, _, _, _) =>
          throw new ImageNotFoundException(imageName.toString)
        case response =>
          unknownResponse(response)
      }.flatten(FlattenStrategy.concat)

    Source(connection.sendRequest(request))
      .via(flow)
  }

  def tag(
    imageId: ImageIdentifier,
    tag: ImageName,
    force: Boolean = false
  )(implicit ec: ExecutionContext): Future[ImageName] = {
    val parameters = Map(
      "repo" -> (tag.registry.fold("")(_ + "/") + tag.namespace.fold("")(_ + "/") + tag.repository),
      "tag" -> tag.tag,
      "force" -> force.toString)

    val request = Post(buildUri(imagesPath / imageId.toString / "tag", Query(parameters)))
    connection.sendRequest(request).flatMap { response =>
      response.status match {
        case StatusCodes.Created =>
          Future.successful(tag)
        case StatusCodes.NotFound =>
          throw new ImageNotFoundException(imageId.toString)
        case _ =>
          // TODO handle errors
          unknownResponseFuture(response)
      }
    }
  }

  def remove(
    imageId: ImageIdentifier,
    force: Boolean,
    prune: Boolean = true
  )(implicit ec: ExecutionContext): Source[RemoveImageMessage, Any] = {
    val query = Query("force" -> force.toString, "noprune" -> (!prune).toString)
    val request = Delete(buildUri(imagesPath / imageId.toString, query))
    val flow: Flow[HttpResponse, RemoveImageMessage, Unit] =
      Flow[HttpResponse].map {
        case HttpResponse(StatusCodes.OK, _, HttpEntity.Chunked(_, chunks), _) =>
          chunks.map(_.data().utf8String).map(str => Json.parse(str).as[RemoveImageMessage])
        case HttpResponse(StatusCodes.OK, _, entity, _) =>
          Source(Unmarshal(entity).to[List[RemoveImageMessage]]).mapConcat[RemoveImageMessage](identity)
        case HttpResponse(StatusCodes.NotFound, _, _, _) =>
          throw new ImageNotFoundException(imageId.toString)
        case response =>
          unknownResponse(response)
      }.flatten(FlattenStrategy.concat)

    Source(connection.sendRequest(request))
      .via(flow)
  }

  def buildFile(
    tarFile: File,
    imageName: Option[ImageName] = None,
    cache: Boolean = true,
    rm: Boolean = true,
    alwaysPull: Boolean = false
  )(implicit ec: ExecutionContext): Source[BuildMessage, Unit] = {
    build(SynchronousFileSource(tarFile), tarFile.length(), imageName, cache, rm, alwaysPull)
  }

  def build(
    tarStream: Source[ByteString, Any],
    tarLength: Long,
    imageName: Option[ImageName] = None,
    cache: Boolean = true,
    rm: Boolean = true,
    alwaysPull: Boolean = false
  )(implicit ec: ExecutionContext): Source[BuildMessage, Unit] = {
    val query = Uri.Query(Map(
      "t" -> imageName.map(_.toString),
      "nocache" -> Some((!cache).toString),
      "rm" -> Some(rm.toString),
      "pull" -> Some(alwaysPull.toString))
      .collect { case (key, Some(value)) => key -> value })
    val request = Post(buildUri(Path / "build", query), HttpEntity(MediaTypes.`application/x-tar`, tarLength, tarStream))

    val flow: Flow[HttpResponse, BuildMessage, Unit] =
      Flow[HttpResponse].map {
        case HttpResponse(StatusCodes.OK, _, HttpEntity.Chunked(_, chunks), _) =>
          chunks.map(_.data().utf8String).map(str => Json.parse(str).as[BuildMessage])
        case response =>
          unknownResponse(response)
      }.flatten(FlattenStrategy.concat)

    Source(connection.sendRequest(request))
      .via(flow)
  }
}
