import sbt._

object Dependencies {
  val akkaVersion = "2.5.4"
  val akkaHttpVersion = "10.0.9"
  val playJsonVersion = "2.6.3"

  val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test"
  )

  val akkaHttp = Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  )

  val json = Seq(
    "com.typesafe.play" %% "play-json" % playJsonVersion
  )

  val misc = Seq(
    "org.kamranzafar" % "jtar" % "2.3",
    "commons-codec" % "commons-codec" % "1.10",
    "com.github.nscala-time" %% "nscala-time" % "2.16.0",
    "org.scalatest" %% "scalatest" % "3.0.2" % "test",
    "ca.juliusdavies" % "not-yet-commons-ssl" % "0.3.11"
  )

  val all =
    akka ++
      akkaHttp ++
      json ++
      misc

  val resolvers = Seq(
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
  )
}
