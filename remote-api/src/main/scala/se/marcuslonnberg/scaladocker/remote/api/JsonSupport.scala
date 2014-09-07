package se.marcuslonnberg.scaladocker.remote.api

import java.lang.reflect.InvocationTargetException

import akka.actor.ActorRefFactory
import akka.http.marshalling.Marshaller
import akka.http.model.{HttpCharsets, HttpEntity, HttpResponse, MediaTypes}
import akka.http.unmarshalling.{Unmarshaller, Unmarshalling}
import akka.stream.FlowMaterializer
import org.json4s.native.Serialization
import org.json4s.{Formats, MappingException}
import se.marcuslonnberg.scaladocker.remote.models.JsonFormats

import scala.concurrent.duration._

trait JsonSupport {
  implicit def json4sFormats: Formats = JsonFormats()

  protected def serialization = Serialization

  implicit def json4sUnmarshallerResponse[T](implicit actorRefFactory: ActorRefFactory, manifest: Manifest[T], materializer: FlowMaterializer): Unmarshaller[HttpResponse, T] =
    Unmarshaller[HttpResponse, T]({
      case x: HttpResponse =>
        json4sUnmarshallerEntity.apply(x.entity)
    })

  implicit def json4sUnmarshallerEntity[T](implicit actorRefFactory: ActorRefFactory, manifest: Manifest[T], materializer: FlowMaterializer): Unmarshaller[HttpEntity, T] =
    Unmarshaller[HttpEntity, T]({
      case x: HttpEntity =>
        import actorRefFactory.dispatcher
        val timeout = 1.second
        x.toStrict(timeout, materializer).map { ent =>
          val value =
            try serialization.read[T](ent.data.utf8String)
            catch {
              case MappingException("unknown error", ite: InvocationTargetException) => throw ite.getCause
            }
          Unmarshalling.Success(value)
        }
    })

  implicit def json4sMarshallerString[T <: AnyRef]: Marshaller[T, String] =
    Marshaller.withFixedCharset[T, String](MediaTypes.`application/json`, HttpCharsets.`UTF-8`)(serialization.write(_))

  implicit def json4sMarshallerEntity[T <: AnyRef]: Marshaller[T, HttpEntity.Regular] =
    Marshaller.withFixedCharset[T, HttpEntity.Regular](MediaTypes.`application/json`, HttpCharsets.`UTF-8`)(c => HttpEntity(serialization.write(c)))
}
