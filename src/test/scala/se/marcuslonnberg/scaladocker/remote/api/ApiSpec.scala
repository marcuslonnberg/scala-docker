package se.marcuslonnberg.scaladocker.remote.api

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Inspectors, Matchers}

trait ApiSpec extends FlatSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll with IntegrationPatience with Inspectors {
  implicit def system: ActorSystem

  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system))

  val client = DockerClient()
}
