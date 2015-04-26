package se.marcuslonnberg.scaladocker.remote.models

import play.api.libs.json.{Format, Json}

package object json extends ContainerFormats with ImageFormats with MessagesFormats {
  implicit val registryAuthConfigFormat: Format[RegistryAuthConfig] = JsonUtils.upperCamelCase(Json.format[RegistryAuthConfig])
}
