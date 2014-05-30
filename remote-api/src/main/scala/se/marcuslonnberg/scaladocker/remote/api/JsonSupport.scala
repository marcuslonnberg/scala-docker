package se.marcuslonnberg.scaladocker.remote.api

import spray.httpx.Json4sSupport
import org.json4s.Formats
import se.marcuslonnberg.scaladocker.remote.models.JsonFormats

trait JsonSupport extends Json4sSupport {
  implicit def json4sFormats: Formats = JsonFormats()
}
