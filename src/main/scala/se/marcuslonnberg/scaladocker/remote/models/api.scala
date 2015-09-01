package se.marcuslonnberg.scaladocker.remote.models

sealed trait ImageTransferMessage

object ImageTransferMessage {

  sealed trait StatusMessage {
    def status: String
  }

  case class Status(status: String) extends ImageTransferMessage with StatusMessage

  case class ImageStatus(status: String, id: ImageId) extends ImageTransferMessage with StatusMessage

  case class Error(error: String) extends ImageTransferMessage

  case class Progress(status: String, id: ImageId, progress: String, progressDetail: ProgressDetail) extends ImageTransferMessage with StatusMessage

  case class ProgressDetail(current: Long, total: Option[Long] = None, start: Option[Long] = None)

}


sealed trait BuildMessage

object BuildMessages {

  case class Output(stream: String) extends BuildMessage {
    def text = stream.trim

    override def toString: String = s"Output($text)"
  }

  case class Error(error: String, errorDetail: ErrorDetail) extends BuildMessage

  case class ErrorDetail(code: Option[Int] = None, message: String)

}

sealed trait RunMessage

object RunMessage {

  case class CreatingImage(message: ImageTransferMessage) extends RunMessage

  case class ContainerStarted(id: ContainerId) extends RunMessage

}


sealed trait RemoveImageMessage

object RemoveImageMessage {
  case class Untagged(imageName: ImageName) extends RemoveImageMessage
  case class Deleted(imageId: ImageIdentifier) extends RemoveImageMessage
}
