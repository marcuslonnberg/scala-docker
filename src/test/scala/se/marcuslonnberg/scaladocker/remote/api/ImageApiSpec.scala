package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import se.marcuslonnberg.scaladocker.remote.models.{CreateImageMessages, ImageName}

class ImageApiSpec extends TestKit(ActorSystem("image-api")) with ApiSpec {
  val imageName = ImageName("scaladocker/image:start")
  val imageNameNewTag = ImageName("scaladocker/image:new-tag")
  val imageNameDelete = ImageName("scaladocker/image:delete")

  override def beforeAll() = {
    val busybox = ImageName("busybox")
    // TODO: client.images.create(busybox)

    client.images.tag(busybox, imageName).futureValue
    client.images.tag(imageName, imageNameDelete).futureValue
  }

  override def afterAll() = {
    client.images.delete(imageNameNewTag).futureValue
  }

  "Image API" should "list images" in {
    currentImageNames() should contain(imageName)
  }

  it should "create (pull) an image" in {
    pending
    val imageName = ImageName("busybox:latest")
    val createStream = client.images.create(imageName)
    val future = Source(createStream).collect {
      case CreateImageMessages.Status(s)
        if s == s"Status: Downloaded newer image for $imageName" ||
          s == s"Status: Image is up to date for $imageName" => true
    }.runWith(Sink.head)

    future.futureValue shouldEqual true
  }

  it should "tag an image" in {
    currentImageNames() should contain(imageName)

    client.images.tag(imageName, imageNameNewTag).futureValue shouldBe true

    currentImageNames() should contain allOf(imageName, imageNameNewTag)
  }

  it should "delete an image" in {
    currentImageNames() should contain(imageNameDelete)

    client.images.delete(imageNameDelete).futureValue shouldBe true

    currentImageNames() should not contain imageNameDelete
  }

  def currentImageNames(): Seq[ImageName] = {
    val images = client.images.list().futureValue
    images.flatMap(_.repoTags)
  }
}
