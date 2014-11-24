package se.marcuslonnberg.scaladocker.remote.models

sealed trait CreateMessage

object CreateMessages {

  sealed trait StatusMessage {
    def status: String
  }

  case class Status(status: String) extends CreateMessage with StatusMessage

  case class ImageStatus(status: String, id: ImageId) extends CreateMessage with StatusMessage

  case class Error(error: String) extends CreateMessage

  case class Progress(status: String, progress: String, progressDetail: ProgressDetail, id: ImageId) extends CreateMessage with StatusMessage

  case class ProgressDetail(current: Long, total: Option[Long] = None, start: Option[Long] = None)

}


sealed trait BuildMessage

object BuildMessages {

  case class Output(stream: String) extends BuildMessage

  case class Error(error: String, errorDetail: ErrorDetail) extends BuildMessage

  case class ErrorDetail(code: Option[Int] = None, message: String)

}
