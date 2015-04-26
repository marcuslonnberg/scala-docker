package se.marcuslonnberg.scaladocker.remote.api

import akka.http.model.StatusCodes
import akka.http.model.Uri.Path

import scala.concurrent.Future

trait HostCommands extends DockerCommands {
  def ping(): Future[Unit] = {
    sendGetRequest(Path / "_ping").map { response =>
      response.status match {
        case StatusCodes.OK => ()
        case statusCode =>
          throw new ServerErrorException(statusCode, "Ping unsuccessful")
      }
    }
  }
}
