package se.marcuslonnberg.scaladocker.remote.models

import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json._
import se.marcuslonnberg.scaladocker.remote.models.json._

import scala.io.Source

class JsonSpec extends FlatSpec with Matchers {

  "ContainerInfo" should "be serializable and deserializable with JSON" in {
    val infoJson = readResource("container-info.json")
    val info = infoJson.as[ContainerInfo]
    compareJsonFromModel(info)
  }

  it should "be serializable and deserializable with JSON (2)" in {
    val infoJson = readResource("container-info-2.json")
    val info = infoJson.as[ContainerInfo]
    compareJsonFromModel(info)
  }

  it should "be serializable and deserializable with JSON (3)" in {
    val infoJson = readResource("container-info-3.json")
    val info = infoJson.as[ContainerInfo]
    compareJsonFromModel(info)
  }

  it should "handle labels" in {
    val infoJson = readResource("container-info-with-labels.json")
    val info = infoJson.as[ContainerInfo]
    compareJsonFromModel(info)
    info.config.labels.get("build.appName") shouldEqual Some("fancy-app")
  }

  it should "handle swarm node" in {
    val infoJson = readResource("container-info-with-swarmnode.json")
    val info = infoJson.as[ContainerInfo]
    compareJsonFromModel(info)
    info.node shouldBe defined
    info.node shouldEqual Some(
      Node(Map("storagedriver"->"aufs"), 16827527168L, 4, "ip-172-31-23-2", "172.31.23.2:2375", "172.31.23.2", "7225:K65R:WGBL:3CKF:47R3:WC72:ADAX:ZJF3:4T2M:PUQW:TNSS:DGJA")
    )
  }


  "ContainerConfig" should "be serializable and deserializable with JSON" in {
    val containerConfigJson = readResource("container-config.json")
    val containerConfig = containerConfigJson.as[ContainerConfig]
    compareJsonFromModel(containerConfig)
  }

  "ContainerStatus" should "be serializable and deserializable with JSON" in {
    val statusJson = readResource("container-status.json")
    val status = statusJson.as[ContainerStatus]
    compareJsonFromModel[ContainerStatus](status)
    status.created shouldEqual new DateTime(1409179933000L)
  }

  it should "handle labels" in {
    val statusJson = readResource("container-status-with-labels.json")
    compareJson[ContainerStatus](statusJson)
    val status = statusJson.as[ContainerStatus]
    status.created shouldEqual new DateTime(1431359918000L)
    status.labels.get("build.appName") shouldEqual Some("fancy-app")
  }

  "Image" should "be serializable and deserializable with JSON" in {
    val imageJson = readResource("image.json")
    val image = imageJson.as[Image]
    compareJsonFromModel[Image](image)
    image.created shouldEqual new DateTime(1409856115000L)
  }

  it should "hanled labels" in {
    val imageJson = readResource("image-with-labels.json")
    val image = imageJson.as[Image]
    compareJsonFromModel[Image](image)
    image.created shouldEqual new DateTime(1431350201000L)
    image.labels.get("build.appName") shouldEqual Some("fancy-app")
  }

  "Volume bindings" should "be serializable and deserializable" in {
    def test(volume: VolumeBinding, string: String) = {
      val jsString = JsString(string)
      Json.toJson(volume) shouldEqual jsString
      jsString.as[VolumeBinding] shouldEqual volume
    }

    test(VolumeBinding(hostPath = "/a", containerPath = "/b", rw = true), "/a:/b")
    test(VolumeBinding(hostPath = "/a", containerPath = "/b", rw = false), "/a:/b:ro")
    test(VolumeBinding(hostPath = "/a/b/c/d/", containerPath = "/e/f/g/h/", rw = true), "/a/b/c/d/:/e/f/g/h/")
  }

  "Image pull messages" should "be serializable and deserializable" in {
    val messagesJson = readResource("pull-image-messages.json")
    val messages = messagesJson.as[Seq[ImageTransferMessage]]

    compareJsonFromModel(messages)
  }

  def readResource(path: String) = {
    val rawString = Source.fromURL(getClass.getResource(path))
    Json.parse(rawString.mkString)
  }

  def compareJson[T: Format](obj: JsValue) {
    val o = obj.as[T]
    val value = Json.toJson(o)
    value shouldEqual obj
  }

  def compareJsonFromModel[T: Format](obj: T) {
    val expectedJson = Json.toJson(obj)
    val value = expectedJson.as[T]
    value shouldEqual obj
  }
}
