package se.marcuslonnberg.scaladocker.remote.api

import akka.http.marshalling.Marshaller
import akka.http.model._
import akka.http.unmarshalling.Unmarshaller
import akka.stream.FlowMaterializer
import play.api.libs.json.{Json, Reads, Writes}

import scala.concurrent.ExecutionContext

trait PlayJsonSupport {
  private[api] implicit def playJsonUnmarshallerResponse[T](implicit ec: ExecutionContext, reader: Reads[T], materializer: FlowMaterializer): Unmarshaller[HttpResponse, T] =
    Unmarshaller[HttpResponse, T]({
      case x: HttpResponse =>
        playJsonUnmarshallerEntity.apply(x.entity)
    })

  private[api] implicit def playJsonUnmarshallerEntity[T](implicit ec: ExecutionContext, reader: Reads[T], materializer: FlowMaterializer): Unmarshaller[HttpEntity, T] =
    Unmarshaller.byteStringUnmarshaller.mapWithCharset { (data, charset) =>
      Json.parse(data.utf8String).as[T]
    }

  private[api] implicit def playJsonMarshallerString[T: Writes]: Marshaller[T, String] =
    Marshaller.withFixedCharset[T, String](MediaTypes.`application/json`, HttpCharsets.`UTF-8`) { c =>
      Json.stringify(Json.toJson(c))
    }

  private[api] implicit def playJsonMarshallerEntity[T: Writes]: Marshaller[T, RequestEntity] =
    Marshaller.withFixedCharset[T, RequestEntity](MediaTypes.`application/json`, HttpCharsets.`UTF-8`) { c =>
      HttpEntity(Json.stringify(Json.toJson(c)))
    }
}
