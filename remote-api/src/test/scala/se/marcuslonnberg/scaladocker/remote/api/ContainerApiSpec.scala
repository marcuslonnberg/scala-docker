package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorSystem
import akka.stream.MaterializerSettings
import akka.stream.scaladsl2.FlowMaterializer
import akka.testkit.TestKit
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FlatSpecLike, Inspectors, Matchers}
import se.marcuslonnberg.scaladocker.remote.models.{ContainerConfig, HostConfig, ImageName}

class ContainerApiSpec extends TestKit(ActorSystem("container-api")) with FlatSpecLike with Matchers with ScalaFutures with IntegrationPatience with Inspectors {
  implicit val mat = FlowMaterializer(MaterializerSettings(system))

  val client = DockerClient()

  val busybox = ImageName("busybox")

  "Container API" should "list containers" in {
    val containerId = client.runLocal(ContainerConfig(busybox, cmd = List("ls", "/")), HostConfig()).futureValue

    val containers = client.containers.list(all = true).futureValue

    forAtLeast(1, containers) { container =>
      container.id shouldEqual containerId
      container.command shouldEqual "ls /"
    }
  }

  it should "get info about a container" in {
    val containerId = client.runLocal(ContainerConfig(busybox, cmd = List("ls", "/")), HostConfig()).futureValue

    val container = client.containers.get(containerId).futureValue

    container.id shouldEqual containerId
    container.path shouldEqual "ls"
    container.args should contain theSameElementsInOrderAs Seq("/")

    container.config.image shouldEqual busybox
  }

  it should "fail when creating a container with an image that does not exist" in {
    val eventualResponse = client.containers.create(ContainerConfig(ImageName("undefined-image")))

    eventualResponse.failed.futureValue shouldBe an[ImageNotFoundException]
  }

  it should "start a container" in {
    val createId = client.containers.create(ContainerConfig(busybox)).futureValue.id

    val hostConfig = HostConfig(publishAllPorts = true)
    val startId = client.containers.start(createId, hostConfig).futureValue

    startId shouldEqual createId

    val info = client.containers.get(createId).futureValue

    info.hostConfig.publishAllPorts shouldEqual true
    info.hostConfig shouldEqual hostConfig
  }
}
