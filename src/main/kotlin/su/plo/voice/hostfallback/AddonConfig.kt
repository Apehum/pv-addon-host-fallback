package su.plo.voice.hostfallback

import com.google.common.base.Predicate
import su.plo.config.Config
import su.plo.config.ConfigField
import su.plo.config.ConfigValidator
import su.plo.config.provider.ConfigurationProvider
import su.plo.config.provider.toml.TomlConfiguration
import java.io.File

@Config
class AddonConfig {
    @ConfigField(
        comment = """
        If player doesn't connect within the specified "connection_window_seconds",
        they will be connected to the next fallback host.
    """,
    )
    val connectionWindowSeconds: Int = 5

    @ConfigField(
        comment = """
            fallback_hosts = ["127.0.0.1:25565"]
        """,
    )
    @ConfigValidator(
        value = HostValidator::class,
        allowed = ["127.0.0.1:25565", "localhost:25565"],
    )
    val fallbackHosts: List<String> = listOf()

    class HostValidator : Predicate<Any> {
        override fun apply(input: Any?): Boolean {
            if (input !is List<*>) return false

            if (!input.all { it is String }) return false

            return input
                .map { it as String }
                .all {
                    val substring = it.split(":")
                    if (substring.size != 2) return false

                    val port = substring[1].toIntOrNull()

                    port != null
                }
        }
    }

    companion object {
        fun loadConfig(configFolder: File): AddonConfig {
            val configFile = File(configFolder, "config.toml")

            val toml = ConfigurationProvider.getProvider<ConfigurationProvider>(TomlConfiguration::class.java)

            return toml
                .load<AddonConfig>(AddonConfig::class.java, configFile, false)
                .also { toml.save(AddonConfig::class.java, it, configFile) }
        }
    }
}
