package se.marcuslonnberg.scaladocker.remote.models

import org.joda.time.DateTime

case class ContainerId(hash: String) {
  def shortHash = hash.take(12)

  override def toString = hash
}

sealed trait Port {
  def port: Int

  def protocol: String

  override def toString = s"$port/$protocol"
}

object Port {

  case class Tcp(port: Int) extends Port {
    def protocol = "tcp"
  }

  case class Udp(port: Int) extends Port {
    def protocol = "udp"
  }

  def apply(port: Int, protocol: String): Option[Port] = {
    protocol match {
      case "tcp" => Some(Tcp(port))
      case "udp" => Some(Udp(port))
      case _ => None
    }
  }

  private val PortFormat = "(\\d+)/(\\w+)".r

  def unapply(raw: String): Option[Port] = {
    raw match {
      case PortFormat(port, "tcp") => Some(Tcp(port.toInt))
      case PortFormat(port, "udp") => Some(Udp(port.toInt))
      case _ => None
    }
  }
}

case class PortBinding(hostIp: String, hostPort: Int)

/**
 * @param memory Memory limit, in bytes.
 * @param memorySwap Total memory usage (memory + swap). Set `-1` to disable swap.
 * @param cpuShares CPU shares (relative weight vs. other containers)
 * @param cpuset Cpuset, examples: `"0-2"`, `"0,1"`
 */
case class ContainerConfig(image: ImageName,
                           hostname: Option[String] = None,
                           domainName: Option[String] = None,
                           user: Option[String] = None,
                           memory: Option[Long] = None,
                           memorySwap: Option[Long] = None,
                           cpuShares: Option[Long] = None,
                           cpuset: Option[String] = None,
                           attachStdin: Option[Boolean] = None,
                           attachStdout: Option[Boolean] = None,
                           attachStderr: Option[Boolean] = None,
                           exposedPorts: Map[Port, Seq[PortBinding]] = Map.empty,
                           tty: Option[Boolean] = None,
                           openStdin: Option[Boolean] = None,
                           stdinOnce: Option[Boolean] = None,
                           env: List[String] = List.empty,
                           cmd: List[String] = List.empty,
                           volumes: List[String] = List.empty,
                           workingDir: Option[String] = None,
                           entryPoint: List[String] = List.empty,
                           networkDisabled: Option[Boolean] = None,
                           onBuild: List[String] = List.empty)

object ContainerLink {
  def unapply(link: String) = {
    link.split(':') match {
      case Array(container) => Some(ContainerLink(container))
      case Array(container, alias) => Some(ContainerLink(container, Some(alias)))
      case _ => None
    }
  }
}

case class ContainerLink(containerName: String, aliasName: Option[String] = None) {
  def mkString = containerName + aliasName.fold("")(":" + _)
}

case class HostConfig(binds: List[String] = List.empty,
                      lxcConf: List[String] = List.empty,
                      privileged: Boolean = false,
                      portBindings: Map[Port, Seq[PortBinding]] = Map.empty,
                      links: List[ContainerLink] = List.empty,
                      publishAllPorts: Boolean = false,
                      dns: List[String] = List.empty,
                      dnsSearch: List[String] = List.empty,
                      volumesFrom: List[String] = List.empty,
                      devices: List[DeviceMapping] = List.empty,
                      networkMode: String = "",
                      capAdd: List[String] = List.empty,
                      capDrop: List[String] = List.empty,
                      restartPolicy: RestartPolicy = RestartPolicy())

case class RestartPolicy(name: String = "", maximumRetryCount: Int = 0)

case class DeviceMapping(pathOnHost: String, pathInContainer: String, cgroupPermissions: String)

case class CreateContainerResponse(id: ContainerId, warnings: List[String])

case class ContainerState(running: Boolean,
                          paused: Boolean,
                          restarting: Boolean,
                          pid: Int,
                          exitCode: Int,
                          startedAt: DateTime,
                          finishedAt: Option[DateTime] = None)

case class NetworkSettings(ipAddress: String,
                           ipPrefixLength: Int,
                           gateway: String,
                           bridge: String,
                           ports: Map[Port, Seq[PortBinding]])

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
                         volumes: List[Volume] = List.empty,
                         hostConfig: HostConfig)

case class Volume(hostPath: String, containerPath: String, rw: Boolean)

case class ContainerStatus(command: String,
                           created: DateTime,
                           id: ContainerId,
                           image: ImageName,
                           names: List[String],
                           ports: Map[Port, Seq[PortBinding]] = Map.empty,
                           status: String)
