import sbt._

object Dependencies {
  val akkaVersion = "2.3.10"
  val akkaHttpStreamsVersion = "1.0-RC3"

  val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-stream-experimental" % akkaHttpStreamsVersion,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaHttpStreamsVersion
  )

  val json = Seq(
    "com.typesafe.play" %% "play-json" % "2.3.9"
  )

  val misc = Seq(
    "org.kamranzafar" % "jtar" % "2.2",
    "commons-codec" % "commons-codec" % "1.10",
    "com.github.nscala-time" %% "nscala-time" % "2.0.0",
    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "ca.juliusdavies" % "not-yet-commons-ssl" % "0.3.11"
  )

  val all =
    akka ++
      json ++
      misc

  val resolvers = Seq(
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
  )
}
