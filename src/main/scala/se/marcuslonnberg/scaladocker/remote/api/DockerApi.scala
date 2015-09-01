package se.marcuslonnberg.scaladocker.remote.api

import akka.http.scaladsl.model.MediaType

object DockerApi {
  object MediaTypes {
    val `application/vnd.docker.raw-stream` = MediaType.custom("application/vnd.docker.raw-stream", MediaType.Encoding.Open)
  }
}
