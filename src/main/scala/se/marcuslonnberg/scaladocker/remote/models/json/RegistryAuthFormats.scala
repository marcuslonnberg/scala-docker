package se.marcuslonnberg.scaladocker.remote.models.json

import play.api.libs.json.{Format, Json}
import se.marcuslonnberg.scaladocker.remote.models.{RegistryAuthConfig, RegistryAuthEntry}

trait RegistryAuthFormats extends CommonFormats {
  implicit val registryAuthConfigFormat: Format[RegistryAuthConfig] = JsonUtils.upperCamelCase(Json.format[RegistryAuthConfig])

  implicit val registryAuthEntryFormat: Format[RegistryAuthEntry] = Json.format[RegistryAuthEntry]
}
