package se.marcuslonnberg.scaladocker.remote.models

import org.joda.time.DateTime
import org.json4s.JObject

case class ContainerId(hash: String) {
  override def toString = hash
}

case class Port(ip: Option[String], privatePort: Option[Int], publicPort: Int, `type`: String)

case class ContainerStatus(command: String,
                           created: DateTime,
                           id: ContainerId,
                           image: ImageName,
                           names: List[String],
                           ports: List[Port],
                           status: String)

case class ContainerConfig(hostname: Option[String] = None,
                           user: Option[String] = None,
                           memory: Option[Double] = None,
                           memorySwap: Option[Double] = None,
                           attachStdin: Option[Boolean] = None,
                           attachStdout: Option[Boolean] = None,
                           attachStderr: Option[Boolean] = None,
                           portSpecs: List[String] = List.empty,
                           tty: Option[Boolean] = None,
                           openStdin: Option[Boolean] = None,
                           stdinOnce: Option[Boolean] = None,
                           env: List[String] = List.empty,
                           cmd: List[String] = List.empty,
                           dns: List[String] = List.empty,
                           image: ImageName,
                           volumes: List[String] = List.empty,
                           volumesFrom: Option[String] = None,
                           workingDir: Option[String] = None,
                           networkDisabled: Option[Boolean] = None,
                           exposedPorts: List[Port] = List.empty)

case class VolumeBind(from: String, to: String)

object ContainerLink {
  def parse(link: String) = {
    link.split(':') match {
      case Array(container) => ContainerLink(container)
      case Array(container, alias) => ContainerLink(container, Some(alias))
      case _ => throw new IllegalArgumentException(s"Could not parse '$link' as a container link")
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
                      privileged: Option[Boolean] = None,
                      dns: Option[String] = None,
                      dnsSearch: Option[String] = None,
                      volumesFrom: Option[String] = None,
                      networkMode: Option[String] = None)

case class CreateContainerResponse(id: ContainerId, warnings: List[String])


case class ContainerState(running: Boolean,
                          pid: Int,
                          exitCode: Int,
                          startedAt: DateTime,
                          ghost: Boolean = false,
                          finishedAt: Option[String] = None)


case class NetworkSettings(ipAddress: String,
                           ipPrefixLen: Int,
                           gateway: String,
                           bridge: String,
                           portMapping: Option[String] = None)


case class ContainerInfo(id: ContainerId,
                         created: DateTime,
                         path: String,
                         args: List[String],
                         config: ContainerConfig,
                         state: ContainerState,
                         image: String,
                         networkSettings: NetworkSettings,
                         resolvConfPath: String,
                         hostnamePath: String,
                         hostsPath: String,
                         name: String,
                         driver: String,
                         execDriver: String,
                         mountLabel: Option[String] = None,
                         processLabel: Option[String] = None,
                         //volumes: List[String],
                         //volumesRW: List[String],
                         hostConfig: HostConfig)
