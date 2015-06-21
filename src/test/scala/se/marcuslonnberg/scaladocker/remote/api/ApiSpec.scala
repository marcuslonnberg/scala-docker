package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorSystem
import akka.stream.{ActorFlowMaterializerSettings, ActorFlowMaterializer}
import org.scalatest.{Inspectors, FlatSpecLike, Matchers, BeforeAndAfterAll}
import org.scalatest.concurrent.{ScalaFutures, IntegrationPatience}

trait ApiSpec extends FlatSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll with IntegrationPatience with Inspectors {
  implicit def system: ActorSystem

  implicit val materializer = ActorFlowMaterializer(ActorFlowMaterializerSettings(system))

  val client = DockerClient()
}
