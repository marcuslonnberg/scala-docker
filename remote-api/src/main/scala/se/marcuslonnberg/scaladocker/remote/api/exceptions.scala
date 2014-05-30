package se.marcuslonnberg.scaladocker.remote.api

import se.marcuslonnberg.scaladocker.remote.models.ContainerId
import spray.http.StatusCode

abstract class DockerApiException(message: String) extends RuntimeException(message)

class ContainerNotFoundException(id: ContainerId) extends DockerApiException(id.hash)

class UnknownResponseException(statusCode: StatusCode) extends DockerApiException(statusCode.value)