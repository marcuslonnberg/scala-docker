import sbt._

object Dependencies {
  val akkaVersion = "2.3.9"
  val akkaHttpStreamsVersion = "1.0-RC1"

  val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-stream-experimental" % akkaHttpStreamsVersion,
    "com.typesafe.akka" %% "akka-http-scala-experimental" % akkaHttpStreamsVersion
  )

  val json = Seq(
    "com.typesafe.play" %% "play-json" % "2.3.8"
  )

  val misc = Seq(
    "org.kamranzafar" % "jtar" % "2.2",
    "commons-codec" % "commons-codec" % "1.10",
    "com.github.nscala-time" %% "nscala-time" % "1.8.0",
    "org.scalatest" %% "scalatest" % "2.2.4" % "test"
  )

  val all =
    akka ++
      json ++
      misc

  val resolvers = Seq(
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
  )
}
