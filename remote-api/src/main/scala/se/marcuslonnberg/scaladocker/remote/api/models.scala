package se.marcuslonnberg.scaladocker.remote.api

case class RegistryAuth(url: String, email: String, password: String) {
  private[api] def toConfig = RegistryAuthConfig(email, password)
}

private[api] case class RegistryAuthConfig(username: String, password: String)
