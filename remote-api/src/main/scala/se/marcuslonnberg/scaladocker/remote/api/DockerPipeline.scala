package se.marcuslonnberg.scaladocker.remote.api

import spray.client.pipelining._
import spray.http.{HttpResponse, Uri, HttpRequest}
import spray.httpx.unmarshalling.FromResponseUnmarshaller
import scala.concurrent.Future
import akka.actor.ActorRefFactory
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
}
