package se.marcuslonnberg.scaladocker.remote.models

import akka.http.scaladsl.model.Uri

case class RegistryAuth(url: Uri, username: String, password: String) {
  def toConfig = RegistryAuthConfig(username, password)
}

case class RegistryAuthConfig(username: String, password: String)

case class RegistryAuthEntry(auth: String)
