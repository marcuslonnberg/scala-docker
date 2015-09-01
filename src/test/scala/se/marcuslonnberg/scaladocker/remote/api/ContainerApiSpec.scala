package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import se.marcuslonnberg.scaladocker.RemoteApiTest
import se.marcuslonnberg.scaladocker.remote._
import se.marcuslonnberg.scaladocker.remote.models._

class ContainerApiSpec extends TestKit(ActorSystem("container-api")) with ApiSpec {
  import system.dispatcher
  val busybox = ImageName("busybox")

  "Container API" should "list containers" taggedAs RemoteApiTest in {
    val containerId = client.runLocal(ContainerConfig(busybox, command = List("ls", "/"))).futureValue

    val containers = client.ps(all = true).futureValue

    forAtLeast(1, containers) { container =>
      container.id shouldEqual containerId
      container.command shouldEqual "ls /"
    }
  }

  it should "get info about a container" taggedAs RemoteApiTest in {
    val containerId = client.runLocal(ContainerConfig(busybox, command = List("ls", "/"))).futureValue

    val container = client.inspect(containerId).futureValue

    container.id shouldEqual containerId
    container.path shouldEqual "ls"
    container.args should contain theSameElementsInOrderAs Seq("/")

    container.config.image shouldEqual busybox
  }

  it should "fail when creating a container with an image that does not exist" taggedAs RemoteApiTest ignore {
    val eventualResponse = client.create(ContainerConfig(ImageName("undefined-image")))

    eventualResponse.failed.futureValue shouldBe an[ImageNotFoundException]
  }

  it should "start a container" taggedAs RemoteApiTest in {
    val createId = client.create(ContainerConfig(busybox)).futureValue.id

    val hostConfig = HostConfig(publishAllPorts = true)
    val startId = client.start(createId, Some(hostConfig)).futureValue

    startId shouldEqual createId

    val info = client.inspect(createId).futureValue

    info.hostConfig.publishAllPorts shouldEqual true
  }

  it should "create, get and delete a container by name" taggedAs RemoteApiTest in {
    val name = ContainerName("scala-container-name")

    client.removeContainer(name).eitherValue
    val createId = client.create(ContainerConfig(busybox), containerName = Some(name)).futureValue.id

    val info = client.inspect(name).futureValue
    info.id shouldEqual createId
    info.name shouldEqual "/scala-container-name"

    client.removeContainer(name).futureValue shouldEqual name
  }

  it should "run a container with environment variables" taggedAs RemoteApiTest in {
    val containerConfig = ContainerConfig(busybox)
      .withEnvironmentVariables("KEY" -> "VALUE")
      .withCommand("/bin/sh", "-c", "echo $KEY")
    val containerId = client.runLocal(containerConfig).futureValue

    val logStream = client.logs(containerId, follow = true)

    val eventualResult = logStream.collect {
      case "VALUE\n" => true
    }.runWith(Sink.head)

    eventualResult.futureValue shouldEqual true

    client.removeContainer(containerId).futureValue shouldEqual containerId
  }
}
