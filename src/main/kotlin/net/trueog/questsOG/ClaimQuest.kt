package net.trueog.questsOG

import kotlinx.coroutines.launch
import net.trueog.questsOG.progression.HomesProgression
import net.trueog.utilitiesog.UtilitiesOG
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ClaimQuest : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val debug = QuestsOG.config.debug
        if (debug) QuestsOG.plugin.logger.info("/claimquest invoked by ${sender.name}")
        if (sender !is Player) {
            sender.sendMessage("ERROR: You can only execute this command as a player.")
            return true
        }

        val nextQuest = HomesProgression.getNextQuest(sender)
        if (debug) QuestsOG.plugin.logger.info("/claimquest nextQuest=${nextQuest?.javaClass?.simpleName}")
        if (nextQuest == null) {
            UtilitiesOG.trueogMessage(sender, "<green>You have completed all available quests.<reset>")
            return true
        }

        QuestsOG.scope.launch {
            try {
                if (debug) QuestsOG.plugin.logger.info("/claimquest coroutine entered for ${sender.name}")
                val isEligible = nextQuest.isEligible(sender)
                if (debug) QuestsOG.plugin.logger.info("/claimquest isEligible=$isEligible")
                if (isEligible == null) {
                    UtilitiesOG.trueogMessage(
                        sender,
                        "<red>Something went wrong while checking your quest eligibility. Contact an administrator.<reset>",
                    )
                    return@launch
                }

                if (isEligible) {
                    val successful = nextQuest.consumeQuestItems(sender)
                    if (!successful) {
                        UtilitiesOG.trueogMessage(
                            sender,
                            "<red>Something wrong while trying to consume the quest items.",
                        )
                        return@launch
                    }
                    nextQuest.reward(sender)
                    val homeCount = HomesProgression.getHomeCount(nextQuest)
                    val questName = nextQuest::class.simpleName
                    UtilitiesOG.logToConsole("[Quests-OG]", "${sender.name} claimed quest $questName")
                    UtilitiesOG.trueogMessage(
                        sender,
                        "<green>Claimed quest! You now have <yellow>$homeCount<green> homes.<reset>",
                    )
                    return@launch
                } else {
                    UtilitiesOG.trueogMessage(sender, "<red>You must meet all the quest's requirements first.<reset>")
                }
            } catch (t: Throwable) {
                QuestsOG.plugin.logger.severe("/claimquest failed: ${t.message}")
                if (debug) t.printStackTrace()
            }
        }
        return true
    }
}
