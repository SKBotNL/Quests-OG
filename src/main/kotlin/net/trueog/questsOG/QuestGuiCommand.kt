package net.trueog.questsOG

import kotlinx.coroutines.launch
import net.trueog.gxui.progress.ProgressMenu
import net.trueog.questsOG.progression.HomesProgression
import net.trueog.questsOG.quests.Quest
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class QuestGuiCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val debug = QuestsOG.config.debug
        if (debug) QuestsOG.plugin.logger.info("/questgui invoked by ${sender.name}")
        if (sender !is Player) {
            sender.sendMessage("ERROR: You can only execute this command as a player.")
            return true
        }

        QuestsOG.scope.launch {
            try {
                if (debug) QuestsOG.plugin.logger.info("/questgui coroutine entered for ${sender.name}")
                val nextQuest = HomesProgression.getNextQuest(sender)
                if (debug) QuestsOG.plugin.logger.info("/questgui nextQuest=${nextQuest?.javaClass?.simpleName}")
                val builder = ProgressMenu.builder(QuestsOG.plugin, sender, "&5Home Quests")

                HomesProgression.quests.forEach { quest ->
                    builder.section(questName(quest), stateFor(quest, nextQuest))
                    quest.getRequirements(sender)?.forEach { requirement ->
                        when (requirement) {
                            is ProgressRequirement ->
                                builder.progress(
                                    materialFor(requirement),
                                    requirement.name,
                                    requirement.current,
                                    requirement.target,
                                )
                            is BooleanRequirement ->
                                builder.goal(materialFor(requirement), requirement.name, requirement.met)
                        }
                    }
                    if (quest == nextQuest) builder.description("&7Use &e/claimquest &7when ready.")
                }

                if (debug) QuestsOG.plugin.logger.info("/questgui dispatching builder.open on main thread")
                MainThreadBlock.runOnMainThread { builder.open() }
                if (debug) QuestsOG.plugin.logger.info("/questgui done")
            } catch (t: Throwable) {
                QuestsOG.plugin.logger.severe("/questgui failed: ${t.message}")
                if (debug) t.printStackTrace()
            }
        }

        return true
    }

    private fun questName(quest: Quest): String = "Home ${HomesProgression.getHomeCount(quest)} Quest"

    private fun stateFor(quest: Quest, nextQuest: Quest?): ProgressMenu.State =
        when {
            nextQuest == null -> ProgressMenu.State.COMPLETE
            quest == nextQuest -> ProgressMenu.State.IN_PROGRESS
            HomesProgression.quests.indexOf(quest) < HomesProgression.quests.indexOf(nextQuest) ->
                ProgressMenu.State.COMPLETE
            else -> ProgressMenu.State.NOT_STARTED
        }

    private fun materialFor(requirement: Requirement): Material =
        when (requirement.name) {
            "Total Shards" -> Material.DIAMOND
            "Hours Played",
            "Days Played" -> Material.CLOCK
            "Blocks Travelled" -> Material.LEATHER_BOOTS
            "Levels" -> Material.EXPERIENCE_BOTTLE
            "Duels Wins" -> Material.IRON_SWORD
            "Beaconator" -> Material.BEACON
            "A Furious Cocktail" -> Material.POTION
            "Serious Dedication" -> Material.NETHERITE_HOE
            "Died to \"death.fell.accident.water\"" -> Material.WATER_BUCKET
            "Blocks Travelled on Pig" -> Material.CARROT_ON_A_STICK
            "Blocks Travelled on Strider" -> Material.WARPED_FUNGUS_ON_A_STICK
            "Dolphins Killed" -> Material.DOLPHIN_SPAWN_EGG
            "Zoglins Killed" -> Material.ZOGLIN_SPAWN_EGG
            "The Cutest Predator" -> Material.AXOLOTL_BUCKET
            "Two by Two" -> Material.WHEAT
            "A Complete Catalogue" -> Material.CAT_SPAWN_EGG
            "Monsters Hunted" -> Material.DIAMOND_SWORD
            "Died to \"fell while climbing\"" -> Material.LADDER
            "Died to \"walked into the danger zone due to Zoglin\"" -> Material.MAGMA_BLOCK
            "Fish Caught" -> Material.FISHING_ROD
            "Has Villager Head" -> Material.PLAYER_HEAD
            "Blocks Walked on Water" -> Material.ICE
            "Blocks Walked Under Water" -> Material.PRISMARINE
            "Music Discs" -> Material.JUKEBOX
            "Finished Advancements" -> Material.RED_CONCRETE
            "Obsidian Mined" -> Material.OBSIDIAN
            "Dragon Eggs" -> Material.DRAGON_EGG
            else -> Material.PAPER
        }
}
