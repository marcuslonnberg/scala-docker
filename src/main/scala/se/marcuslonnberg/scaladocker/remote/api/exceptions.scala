package se.marcuslonnberg.scaladocker.remote.api

import akka.http.model.StatusCode
import se.marcuslonnberg.scaladocker.remote.models.{ContainerId, ImageName}

abstract class DockerApiException(message: String) extends RuntimeException(message)

case class ContainerNotFoundException(id: ContainerId) extends DockerApiException(s"Container $id was not found")

case class ImageNotFoundException(imageName: String) extends DockerApiException(s"Image $imageName was not found")

case class UnknownResponseException(statusCode: StatusCode, entity: String) extends DockerApiException(s"Got unknown status: $statusCode, with entity: $entity")

case class ServerErrorException(statusCode: StatusCode, detailMessage: String) extends DockerApiException(s"Server error ($statusCode}): $detailMessage")

case class BadRequestException(detailMessage: String) extends DockerApiException(detailMessage)

case class CreateImageException(image: ImageName) extends DockerApiException(s"An error occurred while creating image: $image")
