package se.marcuslonnberg.scaladocker.remote.models

import org.joda.time.DateTime

sealed trait ContainerId {
  def value: String
}

case class ContainerHashId(hash: String) extends ContainerId {
  override def value = hash

  def shortHash = hash.take(12)

  override def toString = hash
}

case class ContainerName(name: String) extends ContainerId {
  override def value = name

  override def toString = name
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

object PortBinding {
  def apply(hostPort: Int): PortBinding = new PortBinding(hostPort = hostPort)
}

case class PortBinding(hostIp: String = "0.0.0.0", hostPort: Int)

/**
 * Configuration for a container.
 *
 * @param image Image to run.
 * @param hostname Container hostname.
 * @param domainName Container domain name.
 * @param user Username or UID.
 * @param resourceLimits Resource limits.
 * @param standardStreams Configuration for standard streams.
 * @param exposedPorts Ports that the container should expose.
 * @param env Environment variables.
 * @param cmd Command to run.
 * @param volumes Paths inside the container that should be exposed.
 * @param workingDir Working directory for commands to run in.
 * @param entryPoint Entry point for the container.
 * @param networkDisabled Disable network for the container.
 */
case class ContainerConfig(
  image: ImageName,
  hostname: Option[String] = None,
  domainName: Option[String] = None,
  user: Option[String] = None,
  resourceLimits: ContainerResourceLimits = ContainerResourceLimits(),
  standardStreams: StandardStreamsConfig = StandardStreamsConfig(),
  exposedPorts: Seq[Port] = Seq.empty,
  env: Seq[String] = Seq.empty,
  cmd: Seq[String] = Seq.empty,
  volumes: Seq[String] = Seq.empty,
  workingDir: Option[String] = None,
  entryPoint: Option[Seq[String]] = None,
  labels: Map[String, String] = Map.empty,
  networkDisabled: Boolean = false,
  onBuild: Seq[String] = Seq.empty
)

/**
 * Configuration options for standard streams.
 *
 * @param attachStdIn Attach to standard input.
 * @param attachStdOut Attach to standard output.
 * @param attachStdErr Attach to standard error.
 * @param tty Attach standard streams to a tty.
 * @param openStdin Keep stdin open even if not attached.
 * @param stdinOnce Close stdin when one attached client disconnects.
 */
case class StandardStreamsConfig(
  attachStdIn: Boolean = false,
  attachStdOut: Boolean = false,
  attachStdErr: Boolean = false,
  tty: Boolean = false,
  openStdin: Boolean = false,
  stdinOnce: Boolean = false
)

/**
 * Resource limitations on a container.
 *
 * @param memory Memory limit, in bytes.
 * @param memorySwap Total memory limit (memory + swap), in bytes. Set `-1` to disable swap.
 * @param cpuShares CPU shares (relative weight vs. other containers)
 * @param cpuset CPUs in which to allow execution, examples: `"0-2"`, `"0,1"`.
 */
case class ContainerResourceLimits(
  memory: Long = 0,
  memorySwap: Long = 0,
  cpuShares: Long = 0,
  cpuset: Option[String] = None
)

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

/**
 * @param binds Volume bindings.
 * @param lxcConfig LXC specific configurations.
 * @param privileged Gives the container full access to the host.
 * @param portBindings A map of exposed container ports to bindings on the host.
 * @param links Container links.
 * @param publishAllPorts Allocate a random port for each exposed container port.
 * @param readonlyRootFilesystem Mount the container's root filesystem as read only.
 * @param dnsServers DNS servers for the container to use.
 * @param dnsSearchDomains DNS search domains.
 * @param volumesFrom Volumes to inherit from other containers.
 * @param devices Devices to add to the container.
 * @param networkMode Networking mode for the container
 * @param capAdd Kernel capabilities to add to the container
 * @param capDrop Kernel capabilities to drop from the container.
 * @param restartPolicy Behavior to apply when the container exits.
 */
case class HostConfig(
  binds: Seq[Volume] = Seq.empty,
  lxcConfig: Seq[String] = Seq.empty,
  privileged: Boolean = false,
  portBindings: Map[Port, Seq[PortBinding]] = Map.empty,
  links: Seq[ContainerLink] = Seq.empty,
  publishAllPorts: Boolean = false,
  readonlyRootFilesystem: Boolean = false,
  dnsServers: Seq[String] = Seq.empty,
  dnsSearchDomains: Seq[String] = Seq.empty,
  volumesFrom: Seq[String] = Seq.empty,
  devices: Seq[DeviceMapping] = Seq.empty,
  networkMode: Option[String] = None,
  capAdd: Seq[String] = Seq.empty,
  capDrop: Seq[String] = Seq.empty,
  restartPolicy: RestartPolicy = NeverRestart
)

trait RestartPolicy {
  def name: String
}

case object NeverRestart extends RestartPolicy {
  val name = ""
}

case object AlwaysRestart extends RestartPolicy {
  val name = "always"
}

object RestartOnFailure {
  val name = "on-failure"
}

case class RestartOnFailure(maximumRetryCount: Int = 0) extends RestartPolicy {
  val name = RestartOnFailure.name
}

case class DeviceMapping(pathOnHost: String, pathInContainer: String, cgroupPermissions: String)

case class CreateContainerResponse(id: ContainerHashId, warnings: Seq[String])

case class ContainerState(
  running: Boolean,
  paused: Boolean,
  restarting: Boolean,
  pid: Int,
  exitCode: Int,
  startedAt: Option[DateTime] = None,
  finishedAt: Option[DateTime] = None
)

case class NetworkSettings(
  ipAddress: String,
  ipPrefixLength: Int,
  gateway: String,
  bridge: String,
  ports: Map[Port, Seq[PortBinding]]
)

case class ContainerInfo(
  id: ContainerHashId,
  created: DateTime,
  path: String,
  args: Seq[String],
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
  volumes: Seq[Volume] = Seq.empty,
  hostConfig: HostConfig
)

case class Volume(
  hostPath: String,
  containerPath: String,
  rw: Boolean = true
)

case class ContainerStatus(
  command: String,
  created: DateTime,
  id: ContainerHashId,
  image: ImageName,
  names: Seq[String],
  ports: Map[Port, Seq[PortBinding]] = Map.empty,
  labels: Map[String, String] = Map.empty,
  status: String
)
