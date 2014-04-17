package se.marcuslonnberg.scaladocker.remote.models

import org.joda.time.DateTime
import org.json4s.JObject

case class ContainerId(id: String)

case class Ports(ip: Option[String], privatePort: Option[Int], publicPort: Int, `type`: String)

case class ContainerStatus(command: String,
                           created: DateTime,
                           id: String,
                           image: String,
                           names: List[String],
                           ports: List[Ports],
                           status: String)

case class ContainerConfig(hostname: Option[String] = None,
                           user: Option[String] = None,
                           memory: Option[Double] = None,
                           memorySwap: Option[Double] = None,
                           attachStdin: Option[Boolean] = None,
                           attachStdout: Option[Boolean] = None,
                           attachStderr: Option[Boolean] = None,
                           portSpecs: Option[String] = None,
                           tty: Option[Boolean] = None,
                           openStdin: Option[Boolean] = None,
                           stdinOnce: Option[Boolean] = None,
                           env: List[String] = List.empty,
                           cmd: List[String] = List.empty,
                           dns: Option[String] = None,
                           image: String,
                           volumes: Option[JObject] = None,
                           volumesFrom: Option[String] = None,
                           workingDir: Option[String] = None,
                           networkDisabled: Option[Boolean] = None,
                           exposedPorts: Option[JObject] = None)

object ContainerLink {
  def parse(link: String) = {
    link.split(':') match {
      case Array(container) => ContainerLink(container)
      case Array(container, alias) => ContainerLink(container, Some(alias))
    }
  }
}

case class ContainerLink(containerName: String, aliasName: Option[String] = None) {
  def link = containerName + aliasName.fold("")(":" + _)
}

case class HostConfig(binds: List[String] = List.empty,
                      links: List[ContainerLink] = List.empty,
                      lxcConf: Map[String, String] = Map.empty,
                      portBindings: Option[JObject] = None,
                      publishAllPorts: Option[Boolean] = None,
                      privileged: Option[Boolean] = None)

case class CreateContainerResponse(id: ContainerId, warnings: List[String])

