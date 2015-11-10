scala-docker
============

Scala client using reactive streams (akka-streams) to communicate with [Docker](https://docker.com) hosts.

Setup
-----

Add as a dependency in your `build.sbt`:

```scala
libraryDependencies += "se.marcuslonnberg" %% "scala-docker" % "0.3.0"
```

Cross built for Scala 2.10 and 2.11. 

Usage
-----

### Creating a DockerClient

```scala
implicit val system = ActorSystem()
implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system))

val client = new DockerClient(DockerConnection.fromEnvironment())
```

`DockerConnection.fromEnvironment()` will know which Docker host to connect to by reading `DOCKER_HOST`, `DOCKER_TLS_VERIFY` and `DOCKER_CERT_PATH`.
Host and TLS settings can also be provided as arguments. 

### Running a container

```scala
val imageName = ImageName("fancy-service")
val portBindings: Map[Port, Seq[PortBinding]] = Map(Tcp(8080) -> Seq(PortBinding("0.0.0.0", 8080)))

for {
  containerId: ContainerId <- docker.run(ContainerConfig(imageName), HostConfig(portBindings = portBindings))
  containerInfo: ContainerInfo <- docker.containers.get(containerId)
} yield containerInfo
```
