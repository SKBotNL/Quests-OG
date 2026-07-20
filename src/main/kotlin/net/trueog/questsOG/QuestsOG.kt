package net.trueog.questsOG

import kotlinx.coroutines.*
import me.realized.duels.api.Duels
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import net.luckperms.api.LuckPerms
import net.trueog.diamondbankog.api.DiamondBankAPIKotlin
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin

class QuestsOG : JavaPlugin() {
    companion object {
        lateinit var scope: CoroutineScope

        lateinit var plugin: QuestsOG
        lateinit var config: Config
        lateinit var redis: Redis
        lateinit var diamondBankAPI: DiamondBankAPIKotlin
        lateinit var luckPerms: LuckPerms
        lateinit var duels: Duels
        lateinit var mobHeads: Plugin
        var mm =
            MiniMessage.builder()
                .tags(TagResolver.builder().resolver(StandardTags.color()).resolver(StandardTags.reset()).build())
                .build()

        fun isRedisInitialized() = ::redis.isInitialized
    }

    override fun onEnable() {
        plugin = this
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            logger.severe("Uncaught coroutine exception: ${throwable.message}")
            if (Companion.config.debug) throwable.printStackTrace()
        }
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + exceptionHandler)

        Companion.config =
            Config.create()
                ?: run {
                    Bukkit.getPluginManager().disablePlugin(this)
                    return
                }

        redis = Redis()
        if (!redis.testConnection()) {
            logger.severe("Could not connect to Redis")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        val diamondBankAPIProvider = server.servicesManager.getRegistration(DiamondBankAPIKotlin::class.java)
        if (diamondBankAPIProvider == null) {
            logger.severe("DiamondBank-OG API is null")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }
        diamondBankAPI = diamondBankAPIProvider.provider

        val luckPermsProvider = Bukkit.getServicesManager().getRegistration(LuckPerms::class.java)
        if (luckPermsProvider == null) {
            this.logger.severe("Luckperms API is null, quitting....")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }
        luckPerms = luckPermsProvider.provider

        val duels = Bukkit.getServer().pluginManager.getPlugin("Duels-OG")
        if (duels == null) {
            this.logger.severe("Duels API is null, quitting....")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }
        Companion.duels = duels as Duels

        val mobHeads = Bukkit.getServer().pluginManager.getPlugin("MobHeads-OG")
        if (mobHeads == null) {
            this.logger.severe("The MobHeads-OG plugin is not loaded, quitting....")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }
        Companion.mobHeads = mobHeads

        this.server.pluginManager.registerEvents(Events(), this)
        getCommand("claimquest")?.setExecutor(ClaimQuest())
        getCommand("questgui")?.setExecutor(QuestGuiCommand())
    }

    override fun onDisable() {
        if (isRedisInitialized()) {
            redis.shutdown()
        }

        scope.cancel()

        runBlocking { scope.coroutineContext[Job]?.join() }
    }
}
