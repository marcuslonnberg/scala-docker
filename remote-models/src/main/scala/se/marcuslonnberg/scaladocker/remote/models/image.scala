package se.marcuslonnberg.scaladocker.remote.models

import org.joda.time.DateTime

case class Image(created: DateTime,
                 id: String,
                 parentId: String,
                 repoTags: List[String],
                 size: Double,
                 virtualSize: Double)
