package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{TestKit, TestProbe}
import se.marcuslonnberg.scaladocker.RemoteApiTest
import se.marcuslonnberg.scaladocker.remote.models.{ImageTransferMessage, ImageName, RemoveImageMessage}

import scala.concurrent.duration._

class ImageApiSpec extends TestKit(ActorSystem("image-api")) with ApiSpec {

  import system.dispatcher

  val busybox = ImageName("busybox:latest")

  val imageName = ImageName("scaladocker/image:start")
  val imageNameNewTag = ImageName("scaladocker/image:new-tag")
  val imageNameDelete = ImageName("scaladocker/image:delete")

  override def beforeAll() = {
    client.pullFuture(busybox).futureValue

    client.tag(busybox, imageName, force = true).futureValue
    client.tag(imageName, imageNameDelete, force = true).futureValue

    client.removeImage(imageNameNewTag).runWith(Sink.onComplete(_ => Unit))
  }

  "Image API" should "list images" taggedAs RemoteApiTest in {
    currentImageNames() should contain(imageName)
  }

  it should "pull an image" taggedAs RemoteApiTest in {
    val probe = TestProbe()

    client.pull(busybox).to(Sink.actorRef(probe.ref, "completed")).run()

    probe.fishForMessage(10.seconds) {
      case _: ImageTransferMessage.Status => true
      case _ => false
    }
  }

  it should "tag an image" taggedAs RemoteApiTest in {
    currentImageNames() should contain(imageName)

    client.tag(imageName, imageNameNewTag).futureValue shouldBe imageNameNewTag

    currentImageNames() should contain allOf(imageName, imageNameNewTag)
  }

  it should "delete an image" taggedAs RemoteApiTest in {
    currentImageNames() should contain(imageNameDelete)

    client.removeImage(imageNameDelete)
      .runWith(TestSink.probe[RemoveImageMessage])
      .request(2)
      .expectNext(RemoveImageMessage.Untagged(imageNameDelete))
      .expectComplete()

    currentImageNames() should not contain imageNameDelete
  }

  def currentImageNames(): Seq[ImageName] = {
    val images = client.images().futureValue
    images.flatMap(_.repoTags)
  }
}
