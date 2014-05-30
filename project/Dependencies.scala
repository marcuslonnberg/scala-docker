import sbt._

object Dependencies {
  val akkaVersion = "2.3.2"
  val json4sVersion = "3.2.10"

  val json4sNative = "org.json4s" %% "json4s-native" % json4sVersion
  val json4sExt = "org.json4s" %% "json4s-ext" % json4sVersion

  val nscalaTime = "com.github.nscala-time" %% "nscala-time" % "1.0.0"

  val sprayClient = "io.spray" % "spray-client" % "1.3.1"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion

  val scalaTest = "org.scalatest" %% "scalatest" % "2.1.6" % "test"

  object Projects {
    val remoteApi = Seq(sprayClient, akkaActor, scalaTest)
    val remoteModels = Seq(json4sNative, json4sExt, nscalaTime, scalaTest)
  }

  object Resolvers {
    val spray = "spray repo" at "http://repo.spray.io"
  }

}