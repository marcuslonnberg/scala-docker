package se.marcuslonnberg.scaladocker.remote.api

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.{ExecutionContext, Future}

trait Commands {
  def unknownResponse(response: HttpResponse)(implicit ec: ExecutionContext, mat: Materializer): Source[Nothing, Unit] = {
    import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers._

    Source.apply(Unmarshal(response.entity).to[String](stringUnmarshaller, ec).map { entity =>
      if (response.status == StatusCodes.InternalServerError) {
        throw new ServerErrorException(response.status, entity)
      } else {
        throw new UnknownResponseException(response.status, entity)
      }
    })
  }

  def unknownResponseFuture(response: HttpResponse)(implicit ec: ExecutionContext, mat: Materializer): Future[Nothing] = {
    unknownResponse(response).runWith(Sink.head)
  }
}
