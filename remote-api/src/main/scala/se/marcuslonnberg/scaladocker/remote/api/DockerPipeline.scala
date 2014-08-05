package se.marcuslonnberg.scaladocker.remote.api

import akka.io.IO
import spray.can.Http
import spray.client.pipelining._
import spray.http._
import spray.httpx.unmarshalling.FromResponseUnmarshaller
import scala.collection.immutable.Stack
import scala.concurrent.{Promise, Future}
import akka.actor.{Actor, Props, ActorSystem, ActorRefFactory}
import spray.httpx.marshalling.Marshaller

trait DockerPipeline extends JsonSupport {
  private[api] def baseUri: Uri

  private[api] def getRequest[T](path: Uri.Path, query: Uri.Query = Uri.Query.Empty)(implicit actorRefFactory: ActorRefFactory, manifest: Manifest[T], unmarshaller: FromResponseUnmarshaller[T]) = {
    val uri = baseUri.withPath(path).withQuery(query)
    pipeline(Get(uri))
  }

  private[api] def postRequest[F, T](path: Uri.Path,
                              query: Uri.Query = Uri.Query.Empty,
                              content: F
                               )(implicit actorRefFactory: ActorRefFactory,
                                 manifestTo: Manifest[T],
                                 unmarshaller: FromResponseUnmarshaller[T],
                                 marshaller: Marshaller[F]) = {
    val uri = baseUri.withPath(path).withQuery(query)
    pipeline(Post(uri, content))
  }

  private[api] def pipeline[T](request: HttpRequest)(implicit actorRefFactory: ActorRefFactory, manifest: Manifest[T], unmarshaller: FromResponseUnmarshaller[T]): Future[T] = {
    import actorRefFactory.dispatcher
    val pipeline = sendReceive ~> unmarshal[T]
    pipeline(request)
  }

  private[api] def request(request: HttpRequest)(implicit actorRefFactory: ActorRefFactory): Future[HttpResponse] = {
    import actorRefFactory.dispatcher
    val pipeline = sendReceive
    pipeline(request)
  }

  // TODO: Return streaming data
  def requestChunkedLines(request: HttpRequest)(implicit system: ActorSystem): Future[Seq[String]] = {
    val promise = Promise[Seq[String]]()
    system.actorOf(Props(new Actor {
      IO(Http) ! request

      def receive = {
        case response: HttpResponse =>
          val lines = response.entity.asString.lines.filter(_.nonEmpty)
          val list = lines.toList
          promise.success(list)
        case start: ChunkedResponseStart =>
          val lines = start.message.entity.asString.lines.filter(_.nonEmpty)
          context.become(receiveStream(Stack(lines.toList: _*)))
      }

      def receiveStream(lines: Stack[String]): Receive = {
        case chunk: MessageChunk =>
          val updatedLines = lines ++ chunk.data.asString.lines.filter(_.nonEmpty)
          context.become(receiveStream(updatedLines))
        case end: ChunkedMessageEnd =>
          promise.success(lines)
      }
    }))
    promise.future
  }
}
