Scala-Docker
============

Scala client using reactive streams (akka-streams) to communicate with Docker hosts.

Usage
-----

### Creating a DockerClient

```scala
implicit val system = ActorSystem()
implicit val materializer = ActorFlowMaterializer(ActorFlowMaterializerSettings(system))

val docker = DockerClient()
```

If no host is given to the `DockerClient`, then the host is taken from the environment variable `DOCKER_HOST`.
(No TLS support yet)

### Running a container

```scala
val imageName = ImageName("fancy-service")
val portBindings: Map[Port, Seq[PortBinding]] = Map(Tcp(8080) -> Seq(PortBinding("0.0.0.0", 8080)))

for {
  containerId: ContainerId <- docker.run(ContainerConfig(imageName), HostConfig(portBindings = portBindings))
  containerInfo: ContainerInfo <- docker.containers.get(containerId)
} yield containerInfo
```
