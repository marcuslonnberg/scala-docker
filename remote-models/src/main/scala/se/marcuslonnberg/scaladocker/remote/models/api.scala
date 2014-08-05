package se.marcuslonnberg.scaladocker.remote.models

trait Progress

case class Status(status: String) extends Progress

case class ImageStatus(status: String, id: ImageId) extends Progress

case class Error(error: String) extends Progress

case class ProgressDetail(current: Long, total: Option[Long] = None, start: Option[Long] = None)

case class ProgressStatus(status: String, progress: String, progressDetail: ProgressDetail, id: ImageId) extends Progress
