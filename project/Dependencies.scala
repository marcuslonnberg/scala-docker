import sbt._

object Dependencies {
  val akkaVersion = "2.3.6"
  val akkaHttpStreamsVersion = "0.8"
  val json4sVersion = "3.2.10"

  val json4sNative = "org.json4s" %% "json4s-native" % json4sVersion
  val json4sExt = "org.json4s" %% "json4s-ext" % json4sVersion

  val nscalaTime = "com.github.nscala-time" %% "nscala-time" % "1.4.0"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
  val akkaStream = "com.typesafe.akka" %% "akka-stream-experimental" % akkaHttpStreamsVersion
  val akkaHttp = "com.typesafe.akka" %% "akka-http-experimental" % akkaHttpStreamsVersion

  val jtar = "org.kamranzafar" % "jtar" % "2.2"

  val commonsCodec = "commons-codec" % "commons-codec" % "1.9"

  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.2" % "test"

  val all = Seq(
    akkaActor, akkaStream, akkaHttp, jtar, commonsCodec,
    json4sNative, json4sExt, nscalaTime, scalaTest, akkaTestkit
  )
}
