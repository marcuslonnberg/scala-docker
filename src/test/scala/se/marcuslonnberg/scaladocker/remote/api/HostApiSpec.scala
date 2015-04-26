package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorSystem
import akka.stream.{ActorFlowMaterializer, ActorFlowMaterializerSettings}
import akka.testkit.TestKit
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FlatSpecLike, Inspectors, Matchers}

class HostApiSpec extends TestKit(ActorSystem("host-api")) with FlatSpecLike with Matchers with ScalaFutures with IntegrationPatience with Inspectors {
  implicit val mat = ActorFlowMaterializer(ActorFlowMaterializerSettings(system))

  val client = DockerClient()

  "Host API" should "ping server" in {
    client.host.ping().futureValue shouldEqual ()
  }
}
