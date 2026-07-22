package net.trueog.questsOG

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisConnectionException

class Redis {
    private val redisClient: RedisClient =
        RedisClient.create(QuestsOG.config.redisUrl).apply {
            options =
                io.lettuce.core.ClientOptions.builder()
                    .maintNotificationsConfig(io.lettuce.core.MaintNotificationsConfig.disabled())
                    .build()
        }

    /** @return True if successful */
    fun testConnection(): Boolean {
        try {
            val connection = redisClient.connect()
            connection.close()
            return true
        } catch (_: RedisConnectionException) {
            return false
        }
    }

    fun getValue(key: String): String? {
        val connection = redisClient.connect()
        val commands = connection.sync()
        val value = commands.get(key)
        connection.close()
        return value
    }

    fun setValue(key: String, value: String) {
        val connection = redisClient.connect()
        val commands = connection.sync()
        commands.set(key, value)
        connection.close()
    }

    fun shutdown() {
        redisClient.shutdown()
    }
}
