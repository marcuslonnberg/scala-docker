import Dependencies._

name := "scala-docker"

organization := "se.marcuslonnberg"

version := "0.1.0"

scalaVersion := "2.10.4"

lazy val `remote-models` = project
  .settings(libraryDependencies := Projects.remoteModels: _*)

lazy val `remote-api` = project
  .settings(libraryDependencies := Projects.remoteApi: _*)
  .dependsOn(`remote-models`)

lazy val `scala-docker` = (project in file("."))
  .dependsOn(`remote-api`)

initialCommands in console :=
  """import se.marcuslonnberg.scaladocker.remote.api._
    |import se.marcuslonnberg.scaladocker.remote.models._
    |import akka.actor.ActorSystem
    |import akka.stream.{MaterializerSettings, FlowMaterializer}
    |implicit val system = ActorSystem("scala-docker")
    |import system.dispatcher
    |implicit val mat = FlowMaterializer(MaterializerSettings())""".stripMargin
