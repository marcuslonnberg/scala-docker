package se.marcuslonnberg.scaladocker.remote.api

import java.io.File

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import se.marcuslonnberg.scaladocker.RemoteApiTest
import se.marcuslonnberg.scaladocker.remote.models.{BuildMessages, ImageName}

class BuildImageApiSpec extends TestKit(ActorSystem("build-image-api")) with ApiSpec {
  "Build image API" should "run commands from the Dockerfile in the root of the tar archive" taggedAs RemoteApiTest in {
    val imageName = ImageName("scaladocker/image-build-only-dockerfile")

    val tarArchive = TarArchive(
      inDir = new File(getClass.getResource("build-image-only-dockerfile").getFile),
      out = File.createTempFile("build-image-only-dockerfile", ".tar"))

    val buildOutput = client.images.build(imageName, tarArchive)

    val future = Source(buildOutput).collect {
      case BuildMessages.Output(out) if out.contains("yippee ki yay") => true
    }.runWith(Sink.head)

    future.futureValue shouldEqual true
  }

  it should "build a Dockerimage with files" taggedAs RemoteApiTest in {
    val imageName = ImageName("scaladocker/image-build-simple")

    val tarArchive = TarArchive(
      inDir = new File(getClass.getResource("build-image-simple").getFile),
      out = File.createTempFile("build-image-simple", ".tar"))

    val buildOutput = client.images.build(imageName, tarArchive, noCache = true)

    val future = Source(buildOutput).collect {
      case BuildMessages.Output(out) if out.contains("script") && out.contains("text") => true
    }.runWith(Sink.head)

    future.futureValue shouldEqual true
  }
}
