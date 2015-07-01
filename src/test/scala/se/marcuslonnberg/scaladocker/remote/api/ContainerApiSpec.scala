package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import se.marcuslonnberg.scaladocker.RemoteApiTest
import se.marcuslonnberg.scaladocker.remote.models._

class ContainerApiSpec extends TestKit(ActorSystem("container-api")) with ApiSpec {
  val busybox = ImageName("busybox")

  "Container API" should "list containers" taggedAs RemoteApiTest in {
    val containerId = client.runLocal(ContainerConfig(busybox, cmd = List("ls", "/")), HostConfig()).futureValue

    val containers = client.containers.list(all = true).futureValue

    forAtLeast(1, containers) { container =>
      container.id shouldEqual containerId
      container.command shouldEqual "ls /"
    }
  }

  it should "get info about a container" taggedAs RemoteApiTest in {
    val containerId = client.runLocal(ContainerConfig(busybox, cmd = List("ls", "/")), HostConfig()).futureValue

    val container = client.containers.get(containerId).futureValue

    container.id shouldEqual containerId
    container.path shouldEqual "ls"
    container.args should contain theSameElementsInOrderAs Seq("/")

    container.config.image shouldEqual busybox
  }

  it should "fail when creating a container with an image that does not exist" taggedAs RemoteApiTest in {
    val eventualResponse = client.containers.create(ContainerConfig(ImageName("undefined-image")))

    eventualResponse.failed.futureValue shouldBe an[ImageNotFoundException]
  }

  it should "start a container" taggedAs RemoteApiTest in {
    val createId = client.containers.create(ContainerConfig(busybox)).futureValue.id

    val hostConfig = HostConfig(publishAllPorts = true)
    val startId = client.containers.start(createId, hostConfig).futureValue

    startId shouldEqual createId

    val info = client.containers.get(createId).futureValue

    info.hostConfig.publishAllPorts shouldEqual true
  }

  it should "create, get and delete a container by name" taggedAs RemoteApiTest in {
    val name = ContainerName("scala-container-name")

    client.containers.delete(name).eitherValue
    val createId = client.containers.create(ContainerConfig(busybox), Some(name)).futureValue.id

    val info = client.containers.get(name).futureValue
    info.id shouldEqual createId
    info.name shouldEqual "/scala-container-name"

    client.containers.delete(name).futureValue shouldEqual name
  }

  it should "run a container with environment variables" taggedAs RemoteApiTest in {
    val containerId = client.runLocal(ContainerConfig(busybox, env = Seq("KEY=VALUE"), cmd = List("/bin/sh", "-c", "echo $KEY")), HostConfig()).futureValue

    val logStream = Source(client.containers.logs(containerId, follow = true))

    val eventualResult = logStream.collect {
      case "VALUE\n" => true
    }.runWith(Sink.head)

    eventualResult.futureValue shouldEqual true

    client.containers.delete(containerId).futureValue shouldEqual containerId
  }
}
