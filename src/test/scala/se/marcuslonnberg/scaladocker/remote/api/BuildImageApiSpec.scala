package se.marcuslonnberg.scaladocker.remote.api

import java.io.File

import akka.actor.ActorSystem
import akka.stream.MaterializerSettings
import akka.stream.scaladsl2.{FutureSink, FlowFrom, FlowMaterializer}
import akka.testkit.TestKit
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpecLike}
import se.marcuslonnberg.scaladocker.remote.models.{BuildMessages, ImageName}

class BuildImageApiSpec extends TestKit(ActorSystem("build-image-api")) with FlatSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll with IntegrationPatience {
  implicit val mat = FlowMaterializer(MaterializerSettings(system))

  val client = DockerClient()

  val imageName = ImageName("scaladocker/image-build")

  "Build image API" should "run commands from the Dockerfile in the root of the tar archive" in {
    val tarArchive = TarArchive(
      inDir = new File(getClass.getResource("simple-build-image").getFile),
      out = File.createTempFile("simple-build-image", ".tar"))

    val buildOutput = client.images.build(imageName, tarArchive)

    val future = FutureSink[Boolean]
    val matFlow = FlowFrom(buildOutput).collect {
      case BuildMessages.Output(out) if out.contains("yippee ki yay") => true
    }.withSink(future).run()

    future.future(matFlow).futureValue shouldEqual true
  }
}
