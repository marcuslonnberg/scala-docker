import sbt._

object Dependencies {
  val akkaVersion = "2.4.3"
  val playJsonVersion = "2.5.1"

  val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test"
  )

  val json = Seq(
    "com.typesafe.play" %% "play-json" % playJsonVersion
  )

  val misc = Seq(
    "org.kamranzafar" % "jtar" % "2.3",
    "commons-codec" % "commons-codec" % "1.10",
    "com.github.nscala-time" %% "nscala-time" % "2.12.0",
    "org.scalatest" %% "scalatest" % "2.2.6" % "test",
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
