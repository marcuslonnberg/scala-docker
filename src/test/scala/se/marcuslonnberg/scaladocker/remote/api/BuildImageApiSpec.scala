package se.marcuslonnberg.scaladocker.remote.api

import java.io.File

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import se.marcuslonnberg.scaladocker.remote.models.{BuildMessages, ImageName}

class BuildImageApiSpec extends TestKit(ActorSystem("build-image-api")) with ApiSpec {
  val imageName = ImageName("scaladocker/image-build")

  "Build image API" should "run commands from the Dockerfile in the root of the tar archive" in {
    val tarArchive = TarArchive(
      inDir = new File(getClass.getResource("simple-build-image").getFile),
      out = File.createTempFile("simple-build-image", ".tar"))

    val buildOutput = client.images.build(imageName, tarArchive)

    val future = Source(buildOutput).collect {
      case BuildMessages.Output(out) if out.contains("yippee ki yay") => true
    }.runWith(Sink.head)

    future.futureValue shouldEqual true
  }
}
