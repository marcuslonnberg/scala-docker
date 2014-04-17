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

resolvers += Dependencies.Resolvers.spray
