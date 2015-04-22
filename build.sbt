name := "scala-docker"

organization in ThisBuild := "se.marcuslonnberg"

version := "0.1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.6"

libraryDependencies ++= Dependencies.all
resolvers ++= Dependencies.resolvers

publishMavenStyle := true
