package net.trueog.questsOG

import java.io.File
import org.bukkit.configuration.file.YamlConfiguration

class Config private constructor() {
    lateinit var redisUrl: String
    var debug: Boolean = false

    companion object {
        fun create(): Config? {
            val config = Config()
            val file = File(QuestsOG.plugin.dataFolder, "config.yml")
            if (!file.exists()) {
                QuestsOG.plugin.saveDefaultConfig()
            }
            val yamlConfig = YamlConfiguration.loadConfiguration(file)

            try {
                config.redisUrl = yamlConfig.get("redisUrl") as String
            } catch (_: Exception) {
                QuestsOG.plugin.logger.severe("Failed to parse config option \"redisUrl\" as a string")
                return null
            }

            config.debug = yamlConfig.getBoolean("debug", false)

            return config
        }
    }
}
