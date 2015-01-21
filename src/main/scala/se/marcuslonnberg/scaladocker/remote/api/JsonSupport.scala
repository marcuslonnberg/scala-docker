package se.marcuslonnberg.scaladocker.remote.api

import java.lang.reflect.InvocationTargetException

import akka.http.marshalling.Marshaller
import akka.http.model._
import akka.http.unmarshalling.Unmarshaller
import akka.stream.FlowMaterializer
import org.json4s.native.Serialization
import org.json4s.{Formats, MappingException}
import se.marcuslonnberg.scaladocker.remote.models.JsonFormats

import scala.concurrent.ExecutionContext

trait JsonSupport {
  implicit def json4sFormats: Formats = JsonFormats()

  protected def serialization = Serialization

  implicit def json4sUnmarshallerResponse[T](implicit ec: ExecutionContext, manifest: Manifest[T], materializer: FlowMaterializer): Unmarshaller[HttpResponse, T] =
    Unmarshaller[HttpResponse, T]({
      case x: HttpResponse =>
        json4sUnmarshallerEntity.apply(x.entity)
    })

  implicit def json4sUnmarshallerEntity[T](implicit ec: ExecutionContext, manifest: Manifest[T], materializer: FlowMaterializer): Unmarshaller[HttpEntity, T] =
    Unmarshaller.byteStringUnmarshaller.mapWithCharset { (data, charset) =>
      try serialization.read[T](data.utf8String)
      catch {
        case MappingException("unknown error", ite: InvocationTargetException) => throw ite.getCause
      }
    }

  implicit def json4sMarshallerString[T <: AnyRef]: Marshaller[T, String] =
    Marshaller.withFixedCharset[T, String](MediaTypes.`application/json`, HttpCharsets.`UTF-8`)(serialization.write(_))

  implicit def json4sMarshallerEntity[T <: AnyRef]: Marshaller[T, RequestEntity] =
    Marshaller.withFixedCharset[T, RequestEntity](MediaTypes.`application/json`, HttpCharsets.`UTF-8`)(c => HttpEntity(serialization.write(c)))
}
