package se.marcuslonnberg.scaladocker.remote.models

import org.scalatest.{FlatSpec, Matchers}

class ImageNameSpec extends FlatSpec with Matchers {
  "ImageName" should "parse 'registry.tld:5000/namespace/repository:tag'" in {
    val name = ImageName("registry.tld:5000/namespace/repository:tag")
    name shouldEqual ImageName(
      registry = Some("registry.tld:5000"),
      namespace = Some("namespace"),
      repository = "repository",
      tag = "tag")
  }

  it should "parse 'registry.tld/repository:tag'" in {
    val name = ImageName("registry.tld/repository:tag")
    name shouldEqual ImageName(
      registry = Some("registry.tld"),
      namespace = None,
      repository = "repository",
      tag = "tag")
  }

  it should "parse 'registry:5000/repository:tag'" in {
    val name = ImageName("registry:5000/repository:tag")
    name shouldEqual ImageName(
      registry = Some("registry:5000"),
      namespace = None,
      repository = "repository",
      tag = "tag")
  }

  it should "parse 'localhost/repository:tag'" in {
    val name = ImageName("localhost/repository:tag")
    name shouldEqual ImageName(
      registry = Some("localhost"),
      namespace = None,
      repository = "repository",
      tag = "tag")
  }

  it should "parse 'namespace/repository:tag'" in {
    val name = ImageName("namespace/repository:tag")
    name shouldEqual ImageName(
      registry = None,
      namespace = Some("namespace"),
      repository = "repository",
      tag = "tag")
  }

  it should "parse 'repository:tag'" in {
    val name = ImageName("repository:tag")
    name shouldEqual ImageName(
      registry = None,
      namespace = None,
      repository = "repository",
      tag = "tag")
  }

  it should "parse 'repository'" in {
    val name = ImageName("repository")
    name shouldEqual ImageName(
      registry = None,
      namespace = None,
      repository = "repository")
  }

  it should "not parse 'registry/namespace/repository'" in {
    a[IllegalArgumentException] should be thrownBy {
      ImageName("registry/namespace/repository")
    }
  }

  it should "not parse invalid namespace names" in {
    a[IllegalArgumentException] should be thrownBy ImageName("n/image")
    a[IllegalArgumentException] should be thrownBy ImageName("NAMESPACE/image")
    a[IllegalArgumentException] should be thrownBy ImageName("nAmeSpace/image")
    a[IllegalArgumentException] should be thrownBy ImageName("verylooooooooooooooooooooooooooooooooong/image")
    a[IllegalArgumentException] should be thrownBy ImageName("!@#$/image")
  }

  it should "not parse invalid repository names" in {
    a[IllegalArgumentException] should be thrownBy ImageName("")
    a[IllegalArgumentException] should be thrownBy ImageName("IMAGE")
    a[IllegalArgumentException] should be thrownBy ImageName("iMagE")
    a[IllegalArgumentException] should be thrownBy ImageName("!@#$")
  }

  it should "not parse invalid tag names" in {
    a[IllegalArgumentException] should be thrownBy ImageName("image:verylong" + ("g" * 128))
    a[IllegalArgumentException] should be thrownBy ImageName("image:!@#$")
  }
}
