package se.marcuslonnberg.scaladocker.remote.api

import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri.Path

import scala.concurrent.{ExecutionContext, Future}

class HostCommands(connection: DockerConnection) extends Commands {

  import connection._

  def ping()(implicit ec: ExecutionContext): Future[Unit] = {
    connection.sendRequest(Get(buildUri(Path / "_ping"))).map { response =>
      response.status match {
        case StatusCodes.OK => ()
        case statusCode =>
          throw new ServerErrorException(statusCode, "Ping unsuccessful")
      }
    }
  }
}
