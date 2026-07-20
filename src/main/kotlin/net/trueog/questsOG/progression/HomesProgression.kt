package net.trueog.questsOG.progression

import net.trueog.questsOG.quests.*
import org.bukkit.entity.Player

object HomesProgression : Progression {
    override val quests = arrayOf(HomesTwo(), HomesThree(), HomesFour(), HomesFive(), HomesSix())

    override fun getNextQuest(player: Player): Quest? {
        val homePermission =
            player.effectivePermissions.singleOrNull { permission ->
                permission.permission.startsWith("essentials.sethome.multiple.homes-")
            }
        val nextQuestIndex = homePermission?.permission?.takeLast(1)?.toInt()?.minus(1) ?: 0

        if (nextQuestIndex + 1 > quests.size) {
            return null
        }

        return quests[nextQuestIndex]
    }

    fun getHomeCount(quest: Quest): Int = quests.indexOf(quest) + 2
}
