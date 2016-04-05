import ReleaseKeys._

name := "scala-docker"
organization := "se.marcuslonnberg"
organizationHomepage := Some(url("https://github.com/marcuslonnberg"))

scalaVersion := "2.11.8"
scalacOptions := Seq("-deprecation", "-unchecked", "-feature")

libraryDependencies ++= Dependencies.all
dependencyOverrides += "com.typesafe.play" %% "play-json" % Dependencies.playJsonVersion
resolvers ++= Dependencies.resolvers

licenses := Seq("MIT License" -> url("https://github.com/marcuslonnberg/scala-docker/blob/master/LICENSE"))
homepage := Some(url("https://github.com/marcuslonnberg/scala-docker"))
scmInfo := Some(ScmInfo(url("https://github.com/marcuslonnberg/scala-docker"), "scm:git:git://github.com:marcuslonnberg/scala-docker.git"))

publishMavenStyle := true
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ => false}

pomExtra := {
  <developers>
    <developer>
      <id>marcuslonnberg</id>
      <name>Marcus Lönnberg</name>
      <url>http://marcuslonnberg.se</url>
    </developer>
  </developers>
}

useGpg := true

releasePublishArtifactsAction <<= PgpKeys.publishSigned
