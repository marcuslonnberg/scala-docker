package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorSystem
import akka.testkit.TestKit

class HostApiSpec extends TestKit(ActorSystem("host-api")) with ApiSpec {
  "Host API" should "ping server" in {
    client.host.ping().futureValue shouldEqual (())
  }
}
