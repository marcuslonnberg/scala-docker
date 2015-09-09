package se.marcuslonnberg.scaladocker.remote.api

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.Materializer
import play.api.libs.json.{Json, Reads, Writes}

import scala.concurrent.ExecutionContext

trait PlayJsonSupport {
  implicit def playJsonUnmarshallerEntity[T](implicit ec: ExecutionContext, reader: Reads[T], materializer: Materializer): Unmarshaller[HttpEntity, T] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(MediaTypes.`application/json`)
      .mapWithCharset { (data, charset) =>
      val input = if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
      Json.parse(input).as[T]
    }

  implicit def playJsonMarshallerEntity[T: Writes]: Marshaller[T, MessageEntity] =
    Marshaller.StringMarshaller.wrap(ContentTypes.`application/json`)(str => Json.stringify(Json.toJson(str)))
}

object PlayJsonSupport extends PlayJsonSupport
