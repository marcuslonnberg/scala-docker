package se.marcuslonnberg.scaladocker.remote.api

import akka.http.model.HttpMethods._
import akka.http.model.Uri.Path
import akka.http.model.{HttpRequest, StatusCodes}
import akka.http.unmarshalling._
import akka.stream.scaladsl.{Sink, Source}
import org.reactivestreams.Publisher
import se.marcuslonnberg.scaladocker.remote.models.{CreateImageMessage, Image, ImageName}
import se.marcuslonnberg.scaladocker.remote.models.json._

import scala.concurrent.Future

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

  def create(imageName: ImageName): Publisher[CreateImageMessage] = {
    val parameters = Map("fromImage" -> imageName.toString)

    val uri = baseUri
      .withPath(Path / "images" / "create")
      .withQuery(parameters)

    val headers = getAuthHeader(imageName.registry).toList

    Source(requestChunkedLinesJson[CreateImageMessage](HttpRequest(POST, uri, headers)))
      .runWith(Sink.publisher[CreateImageMessage])
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
