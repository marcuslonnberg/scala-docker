package se.marcuslonnberg.scaladocker.remote.models

import org.joda.time.DateTime
import org.json4s.Extraction
import org.json4s.native.Serialization._
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json._
import se.marcuslonnberg.scaladocker.remote.models.playjson._
import scala.io.Source

class JsonSpec extends FlatSpec with Matchers {

  implicit val formats = JsonFormats()

  "ContainerInfo" should "be serializable and deserializable with JSON" in {
    val infoJson = readResourcePlay("container-info.json")
    val info = infoJson.as[ContainerInfo]
    compareJson2(info)
  }

  "ContainerConfig" should "be serializable and deserializable with JSON" in {
    val containerConfigJson = readResourcePlay("container-config.json")
    val containerConfig = containerConfigJson.as[ContainerConfig]
    compareJson2(containerConfig)
  }

  "ContainerStatus" should "be serializable and deserializable with JSON" in {
    val statusJson = readResourcePlay("container-status.json")
    compareJson[ContainerStatus](statusJson)
    val status = statusJson.as[ContainerStatus]
    status.created shouldEqual new DateTime(1409179933000L)
  }

  "Image" should "be serializable and deserializable with JSON" in {
    val imageJson = readResourcePlay("image.json")
    compareJson[Image](imageJson)
    val image = imageJson.as[Image]
    image.created shouldEqual new DateTime(1409856115000L)
  }

  "Volume bindings" should "be serializable and deserializable" in {
    def test(volume: Volume, string: String) = {
      val jsString = JsString(string)
      Json.toJson(volume) shouldEqual jsString
      jsString.as[Volume] shouldEqual volume
    }

    test(Volume(hostPath = "/a", containerPath = "/b", rw = true), "/a:/b")
    test(Volume(hostPath = "/a", containerPath = "/b", rw = false), "/a:/b:ro")
    test(Volume(hostPath = "/a/b/c/d/", containerPath = "/e/f/g/h/", rw = true), "/a/b/c/d/:/e/f/g/h/")
  }

  def readResourcePlay(path: String) = {
    val rawString = Source.fromURL(getClass.getResource(path))
    Json.parse(rawString.mkString)
  }

  def readResource[T: Manifest](path: String): T = {
    val rawString = Source.fromURL(getClass.getResource(path))
    read[T](rawString.mkString)
  }

  def compare[T: Manifest](obj: T) {
    val expected = Extraction.decompose(obj)
    val value = Extraction.decompose(expected.extract[T])
    value shouldEqual expected
  }

  def compareJson[T: Format](obj: JsValue) {
    val o = obj.as[T]
    val value = Json.toJson(o)
    value shouldEqual obj
  }

  def compareJson2[T: Format](obj: T) {
    val expectedJson = Json.toJson(obj)
    val value = expectedJson.as[T]
    value shouldEqual obj
  }
}
