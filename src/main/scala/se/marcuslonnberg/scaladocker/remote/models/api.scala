package se.marcuslonnberg.scaladocker.remote.models

sealed trait CreateImageMessage

object CreateImageMessages {

  sealed trait StatusMessage {
    def status: String
  }

  case class Status(status: String) extends CreateImageMessage with StatusMessage

  case class ImageStatus(status: String, id: ImageId) extends CreateImageMessage with StatusMessage

  case class Error(error: String) extends CreateImageMessage

  case class Progress(status: String, id: ImageId, progress: String, progressDetail: ProgressDetail) extends CreateImageMessage with StatusMessage

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
