package se.marcuslonnberg.scaladocker.remote

import com.kifi.macros.json

package object api {
  case class RegistryAuth(url: String, username: String, password: String) {
    def toConfig = RegistryAuthConfig(username, password)
  }

  @json case class RegistryAuthConfig(username: String, password: String)
}
