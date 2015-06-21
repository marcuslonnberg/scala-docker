package se.marcuslonnberg.scaladocker.remote.api

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

import scala.collection.convert.wrapAsScala._

object FileUtils {
  def listFilesRecursive(base: File): Map[String, File] = {
    def traverse(source: File, baseDestination: String): Map[String, File] = {
      val fileMappings = source.listFiles() match {
        case null =>
          Map.empty[String, File]
        case files =>
          files.flatMap { file =>
            val path = baseDestination + file.getName
            if (file.isDirectory) {
              traverse(file, path + "/") + (path -> file)
            } else {
              Map(path -> file)
            }
          }.toMap
      }
      fileMappings
    }
    traverse(base, baseDestination = "/")
  }

  def filePermissions(file: File): Int = {
    val permissions = Files.getPosixFilePermissions(file.toPath)
    permissions.toSeq.map(_.toInt).sum
  }

  implicit class RichPosixFilePermission(val permission: PosixFilePermission) extends AnyVal {
    def toInt: Int = {
      permission match {
        case PosixFilePermission.OTHERS_EXECUTE => 1
        case PosixFilePermission.OTHERS_READ => 2
        case PosixFilePermission.OTHERS_WRITE => 4
        case PosixFilePermission.GROUP_EXECUTE => 10
        case PosixFilePermission.GROUP_READ => 20
        case PosixFilePermission.GROUP_WRITE => 40
        case PosixFilePermission.OWNER_EXECUTE => 100
        case PosixFilePermission.OWNER_READ => 200
        case PosixFilePermission.OWNER_WRITE => 400
      }
    }
  }

}
