package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorSystem
import akka.http.Http
import akka.http.Http.{Connect, OutgoingConnection}
import akka.http.marshalling.Marshaller
import akka.http.marshalling.Marshalling.WithFixedCharset
import akka.http.model.Uri.{Path, Query}
import akka.http.model._
import akka.http.unmarshalling.FromResponseUnmarshaller
import akka.io.IO
import akka.pattern.ask
import akka.stream.scaladsl2._
import akka.util.Timeout
import org.reactivestreams.Publisher

import scala.concurrent.Future
import scala.concurrent.duration._

trait DockerPipeline extends JsonSupport {
  private[api] def baseUri: Uri

  private[api] def createUri(path: Path, query: Query): Uri = {
    baseUri.withPath(baseUri.path ++ path).withQuery(query)
  }

  private[api] def getRequest[T](path: Uri.Path, query: Uri.Query = Uri.Query.Empty)
                                (implicit system: ActorSystem,
                                 manifest: Manifest[T],
                                 unmarshaller: FromResponseUnmarshaller[T],
                                 materializer: FlowMaterializer) = {
    val uri = createUri(path, query)
    sendAndUnmarhall(HttpRequest(HttpMethods.GET, uri))
  }

  private[api] def postRequest[F, T](path: Uri.Path,
                                     query: Uri.Query = Uri.Query.Empty,
                                     content: F)
                                    (implicit system: ActorSystem,
                                     manifestTo: Manifest[T],
                                     unmarshaller: FromResponseUnmarshaller[T],
                                     marshaller: Marshaller[F, HttpEntity.Regular],
                                     materializer: FlowMaterializer): Future[T] = {
    import system.dispatcher
    val uri = createUri(path, query)
    val eventualEntity = marshaller(content).map {
      case marshalling: WithFixedCharset[HttpEntity.Regular] =>
        marshalling.marshal()
      case marshalling =>
        sys.error(s"Unsupported marshalling: $marshalling")
    }
    eventualEntity.toFuture.flatMap { entity =>
      sendAndUnmarhall(HttpRequest(HttpMethods.POST, uri, entity = entity))
    }
  }

  private[api] def sendAndUnmarhall[T](httpRequest: HttpRequest)
                                      (implicit system: ActorSystem,
                                       manifest: Manifest[T],
                                       unmarshaller: FromResponseUnmarshaller[T],
                                       materializer: FlowMaterializer): Future[T] = {
    import system.dispatcher
    sendRequest(httpRequest).flatMap { response =>
      unmarshaller(response).toFuture
    }
  }

  private[api] def sendRequest(request: HttpRequest)
                              (implicit system: ActorSystem, materializer: FlowMaterializer): Future[HttpResponse] = {
    implicit val timeout = Timeout(30.seconds)
    import system.dispatcher

    (IO(Http) ? Connect(request.uri.authority.host.address(), request.uri.effectivePort)).mapTo[OutgoingConnection]
      .flatMap { connection =>

      FlowFrom(List(request -> 'NoContext)).publishTo(connection.processor)

      val responseSink = FutureSink[HttpResponse]
      val materializedFlow = FlowFrom(connection.processor).map(_._1).withSink(responseSink).run()
      responseSink.future(materializedFlow)
    }
  }

  private[api] def requestChunkedLines(httpRequest: HttpRequest)
                                      (implicit system: ActorSystem, materializer: FlowMaterializer): Publisher[String] = {
    import system.dispatcher

    val sub = SubscriberSource[HttpEntity.ChunkStreamPart]()
    val out = PublisherSink[String]

    val flow = FlowFrom[HttpEntity.ChunkStreamPart].withSource(sub).map { chunk =>
      chunk.data().utf8String
    }.withSink(out).run()

    val subscriber = sub.subscriber(flow)

    sendRequest(httpRequest).map { response =>
      response.entity match {
        case HttpEntity.Chunked(contentType, chunks) =>
          chunks.subscribe(subscriber)
        case entity =>
          subscriber.onError {
            sys.error(s"Unsupported entity: $entity")
          }
      }
    }

    out.publisher(flow)
  }
}
