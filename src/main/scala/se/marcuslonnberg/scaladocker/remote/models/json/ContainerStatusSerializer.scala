package se.marcuslonnberg.scaladocker.remote.models.json

import org.joda.time.DateTime
import org.json4s.{Extraction, CustomSerializer}
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import se.marcuslonnberg.scaladocker.remote.models.JsonFormatHelpers._
import se.marcuslonnberg.scaladocker.remote.models._

object ContainerStatusSerializer extends CustomSerializer[ContainerStatus](implicit formats => ( {
  case obj: JObject =>
    implicit val o = obj

    val command = extractField[String]("Command")
    val created = new DateTime(extractField[Long]("Created") * 1000)
    val id = extractField[ContainerHashId]("Id")
    val image = extractField[ImageName]("Image")
    val names = extractField[List[String]]("Names")
    val status = extractField[String]("Status")

    val ports = extractField[List[JObject]]("Ports").map { implicit o =>
      val containerPort = extractField[Int]("PrivatePort")
      val protocol = extractField[String]("Type")
      val hostPort = extractFieldOpt[Int]("PublicPort")
      val hostIp = extractFieldOpt[String]("IP")

      val bindings = (hostIp, hostPort) match {
        case (Some(hostIp), Some(hostPort)) => Seq(PortBinding(hostIp, hostPort))
        case _ => Seq.empty[PortBinding]
      }

      Port(containerPort, protocol).get -> bindings
    }.groupBy(_._1).mapValues(a => a.flatMap(_._2))

    ContainerStatus(
      command = command,
      created = created,
      id = id,
      image = image,
      names = names,
      ports = ports,
      status = status)
}, {
  case cs: ContainerStatus =>
    val ports = cs.ports.map {
      case (port, bindings) =>
        val base = ("PrivatePort" -> port.port) ~
          ("Type" -> port.protocol)

        if (bindings.isEmpty) {
          Seq(base)
        } else {
          bindings.map { binding =>
            base ~
              ("PublicPort" -> binding.hostPort) ~
              ("IP" -> binding.hostIp)
          }
        }
    }.flatten

    ("Command" -> cs.command) ~
      ("Created" -> cs.created.getMillis / 1000) ~
      ("Id" -> Extraction.decompose(cs.id)) ~
      ("Image" -> Extraction.decompose(cs.image)) ~
      ("Names" -> cs.names) ~
      ("Ports" -> ports) ~
      ("Status" -> cs.status)
}))
