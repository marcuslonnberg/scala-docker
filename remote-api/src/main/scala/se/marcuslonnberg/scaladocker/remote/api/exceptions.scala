package se.marcuslonnberg.scaladocker.remote.api

import akka.http.model.StatusCode
import se.marcuslonnberg.scaladocker.remote.models.{ImageName, ContainerId}

abstract class DockerApiException(message: String) extends RuntimeException(message)

class ContainerNotFoundException(id: ContainerId) extends DockerApiException(s"Container $id was not found")

class ImageNotFoundException(imageName: String) extends DockerApiException(s"Image $imageName was not found")

class UnknownResponseException(statusCode: StatusCode) extends DockerApiException(statusCode.value)

class CreateImageException(image: ImageName) extends DockerApiException(s"An error occurred while creating image: $image")
