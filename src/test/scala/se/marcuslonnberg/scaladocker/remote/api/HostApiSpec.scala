package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorSystem
import akka.testkit.TestKit
import se.marcuslonnberg.scaladocker.RemoteApiTest

class HostApiSpec extends TestKit(ActorSystem("host-api")) with ApiSpec {
  "Host API" should "ping server" taggedAs RemoteApiTest in {
    client.host.ping().futureValue shouldEqual (())
  }
}
