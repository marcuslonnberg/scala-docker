import Dependencies._

name := "scala-docker"

organization in ThisBuild := "se.marcuslonnberg"

version := "0.1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.6"

libraryDependencies ++= all

initialCommands in console :=
  """import se.marcuslonnberg.scaladocker.remote.api._
    |import se.marcuslonnberg.scaladocker.remote.models._
    |import akka.actor.ActorSystem
    |import akka.stream.{MaterializerSettings, FlowMaterializer}
    |implicit val system = ActorSystem("scala-docker")
    |import system.dispatcher
    |implicit val mat = FlowMaterializer(MaterializerSettings())""".stripMargin

publishMavenStyle := true
