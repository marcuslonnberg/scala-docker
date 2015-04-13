package se.marcuslonnberg.scaladocker.remote.models

import org.json4s.Extraction
import org.json4s.native.Serialization._
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.{Json, Reads}
import se.marcuslonnberg.scaladocker.remote.models.playjson._
import scala.io.Source

class JsonSpec extends FlatSpec with Matchers {

  implicit val formats = JsonFormats()

  "ContainerInfo" should "be serializable and deserializable with JSON" in {
    val info = readResource[ContainerInfo]("container-info.json")
    compare(info)
  }

  "ContainerStatus" should "be serializable and deserializable with JSON" in {
    val state = readResourcePlay[ContainerStatus]("container-status.json")
    compare(state)
  }

  "Image" should "be serializable and deserializable with JSON" in {
    val state = readResourcePlay[Image]("image.json")
    compare(state)
  }

  "Volume bindings" should "be serializable and deserializable" in {
    def test(volume: Volume, string: String) = {
      JsonFormats.serializeBinding(volume) shouldEqual string
      JsonFormats.deserializeBinding(string) shouldEqual volume
    }

    test(Volume(hostPath = "/a", containerPath = "/b", rw = true), "/a:/b")
    test(Volume(hostPath = "/a", containerPath = "/b", rw = false), "/a:/b:ro")
    test(Volume(hostPath = "/a/b/c/d/", containerPath = "/e/f/g/h/", rw = true), "/a/b/c/d/:/e/f/g/h/")
  }

  def readResourcePlay[T: Reads](path: String): T = {
    val rawString = Source.fromURL(getClass.getResource(path))
    Json.parse(rawString.mkString).as[T]
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
}
