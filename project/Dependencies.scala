import sbt._

object Dependencies {
  val akkaVersion = "2.3.2"
  val json4sVersion = "3.2.8"

  val json4sNative = "org.json4s" %% "json4s-native" % json4sVersion
  val json4sExt = "org.json4s" %% "json4s-ext" % json4sVersion

  val nscalaTime = "com.github.nscala-time" %% "nscala-time" % "0.8.0"

  val sprayClient = "io.spray" % "spray-client" % "1.3.1"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion

  object Projects {
    val remoteModels = Seq(json4sNative, json4sExt, nscalaTime)
    val remoteApi = Seq(sprayClient, akkaActor)
  }

  object Resolvers {
    val spray = "spray repo" at "http://repo.spray.io"
  }

}