package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorSystem
import akka.http.Http
import akka.http.Http.{Connect, OutgoingConnection}
import akka.http.marshalling.Marshaller
import akka.http.marshalling.Marshalling.WithFixedCharset
import akka.http.model._
import akka.http.unmarshalling.FromResponseUnmarshaller
import akka.io.IO
import akka.pattern.ask
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._

trait DockerPipeline extends JsonSupport {
  private[api] def baseUri: Uri

  private[api] def getRequest[T](path: Uri.Path, query: Uri.Query = Uri.Query.Empty)
                                (implicit system: ActorSystem,
                                 manifest: Manifest[T],
                                 unmarshaller: FromResponseUnmarshaller[T],
                                 materializer: FlowMaterializer) = {
    val uri = baseUri.withPath(path).withQuery(query)
    sendAndUnmarhall(HttpRequest(HttpMethods.GET, uri))
  }

  private[api] def postRequest[F, T](path: Uri.Path,
                                     query: Uri.Query = Uri.Query.Empty,
                                     content: F)
                                    (implicit system: ActorSystem,
                                     manifestTo: Manifest[T],
                                     unmarshaller: FromResponseUnmarshaller[T],
                                     marshaller: Marshaller[F, HttpEntity.Regular],
                                     materializer: FlowMaterializer) = {
    import system.dispatcher
    val uri = baseUri.withPath(path).withQuery(query)
    val eventualEntity = marshaller(content).map {
      case marshalling: WithFixedCharset[HttpEntity.Regular] =>
        marshalling.marshal()
      case marshalling =>
        sys.error(s"Unsupported marshalling: $marshalling")
    }
    eventualEntity.flatMap { entity =>
      sendAndUnmarhall(HttpRequest(HttpMethods.POST, uri, entity = entity))
    }
  }

  private[api] def sendAndUnmarhall[T](httpRequest: HttpRequest)
                                      (implicit system: ActorSystem,
                                       manifest: Manifest[T],
                                       unmarshaller: FromResponseUnmarshaller[T],
                                       materializer: FlowMaterializer): Future[T] = {
    import system.dispatcher
    sendRequest(httpRequest).flatMap { a =>
      unmarshaller(a)
    }.map(_.value)
  }

  private[api] def sendRequest(request: HttpRequest)
                              (implicit system: ActorSystem, materializer: FlowMaterializer): Future[HttpResponse] = {
    implicit val timeout = Timeout(30.seconds)
    import system.dispatcher

    (IO(Http) ? Connect(request.uri.authority.host.address(), request.uri.effectivePort)).mapTo[OutgoingConnection]
      .flatMap { connection =>
      Flow(List(request -> 'NoContext)).produceTo(connection.processor)
      Flow(connection.processor).map(_._1).toFuture()
    }
  }

  private[api] def requestChunkedLines(httpRequest: HttpRequest)
                                      (implicit system: ActorSystem, materializer: FlowMaterializer): Future[Flow[String]] = {
    import system.dispatcher
    sendRequest(httpRequest).map { response =>
      response.entity match {
        case HttpEntity.Chunked(contentType, chunks) =>
          Flow(chunks)
            .map(_.data().utf8String)
        case entity =>
          sys.error(s"Unsupported entity: $entity")
      }
    }
  }
}
