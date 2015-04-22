package se.marcuslonnberg.scaladocker.remote.models

case class RegistryAuth(url: String, username: String, password: String) {
  def toConfig = RegistryAuthConfig(username, password)
}

case class RegistryAuthConfig(username: String, password: String)
