import sbt._

object Dependencies {
  val akkaVersion = "2.3.12"
  val akkaHttpStreamsVersion = "1.0"
  val playJsonVersion = "2.3.10"

  val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-stream-experimental" % akkaHttpStreamsVersion,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaHttpStreamsVersion,
    "com.typesafe.akka" %% "akka-stream-testkit-experimental" % akkaHttpStreamsVersion
  )

  val json = Seq(
    "com.typesafe.play" %% "play-json" % playJsonVersion,
    "de.heikoseeberger" %% "akka-http-play-json" % "1.0.0"
  )

  val misc = Seq(
    "org.kamranzafar" % "jtar" % "2.2",
    "commons-codec" % "commons-codec" % "1.10",
    "com.github.nscala-time" %% "nscala-time" % "2.2.0",
    "org.scalatest" %% "scalatest" % "2.2.5" % "test",
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
