package net.trueog.questsOG.quests

import net.luckperms.api.node.types.PermissionNode
import net.trueog.questsOG.MainThreadBlock.runOnMainThread
import net.trueog.questsOG.ProgressRequirement
import net.trueog.questsOG.QuestsOG
import net.trueog.questsOG.Requirement
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Statistic
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class HomesSix : Quest {
    private data class Requirements(
        val totalShards: Long,
        val ticksPlayed: Int,
        val walkOnWaterOneCm: Int,
        val walkUnderWaterOneCm: Int,
        val stoneLikePickedUp: Int,
        val discs: Int,
        //        val hasLeftConfinesOfWorldWhileFightingEnderDragon: Boolean,
        val finishedAdvancements: Int,
        val obsidianMined: Int,
        val dragonEggs: Int,
        val levels: Int,
        val duelsWins: Int,
    )

    private val neededDiscs =
        setOf(
            Material.MUSIC_DISC_13,
            Material.MUSIC_DISC_CAT,
            Material.MUSIC_DISC_BLOCKS,
            Material.MUSIC_DISC_CHIRP,
            Material.MUSIC_DISC_FAR,
            Material.MUSIC_DISC_MALL,
            Material.MUSIC_DISC_MELLOHI,
            Material.MUSIC_DISC_STAL,
            Material.MUSIC_DISC_STRAD,
            Material.MUSIC_DISC_WARD,
            Material.MUSIC_DISC_11,
            Material.MUSIC_DISC_WAIT,
            Material.MUSIC_DISC_5,
        )

    private suspend fun fetchRequirements(player: Player): Requirements? {
        val totalShards =
            QuestsOG.diamondBankAPI.getTotalShards(player.uniqueId).getOrElse {
                return null
            }

        val ticksPlayed = player.getStatistic(Statistic.PLAY_ONE_MINUTE)

        val walkOnWaterOneCm = player.getStatistic(Statistic.WALK_ON_WATER_ONE_CM)

        val walkUnderWaterOneCm = player.getStatistic(Statistic.WALK_UNDER_WATER_ONE_CM)

        val stoneMined = player.getStatistic(Statistic.MINE_BLOCK, Material.STONE)
        val cobblestoneMined = player.getStatistic(Statistic.MINE_BLOCK, Material.COBBLESTONE)
        val stoneLikeMined = stoneMined + cobblestoneMined

        val discs = player.inventory.filterNotNull().filter { it.type in neededDiscs }.distinctBy { it.type }

        //        val hasLeftConfinesOfWorldWhileFightingEnderDragon =
        //
        // QuestsOG.redis.getValue("questsog:${player.uniqueId}:deaths:leftConfinesOfWorldWhileFightingEnderDragon") ==
        // "true"

        val finishedAdvancements =
            Bukkit.getServer().advancementIterator().asSequence().filterNotNull().filter {
                player.getAdvancementProgress(it).isDone
            }

        val obsidianMined = player.getStatistic(Statistic.MINE_BLOCK, Material.OBSIDIAN)

        val dragonEggs = player.inventory.all(Material.DRAGON_EGG).values.sumOf { it.amount }

        val duelsWins = QuestsOG.duels.userManager.get(player.uniqueId)?.wins ?: 0

        return Requirements(
            totalShards,
            ticksPlayed,
            walkOnWaterOneCm,
            walkUnderWaterOneCm,
            stoneLikeMined,
            discs.size,
            //            hasLeftConfinesOfWorldWhileFightingEnderDragon,
            finishedAdvancements.count(),
            obsidianMined,
            dragonEggs,
            player.level,
            duelsWins,
        )
    }

    override suspend fun isEligible(player: Player): Boolean? {
        val requirements = fetchRequirements(player)

        if (requirements == null) {
            return null
        }

        return requirements.totalShards >= 5000L * 9 &&
            requirements.ticksPlayed / 20.0 / 60.0 / 60.0 / 24.0 >= 30 &&
            requirements.walkOnWaterOneCm / 100000.0 >= 10 &&
            requirements.walkUnderWaterOneCm / 100000.0 >= 10 &&
            requirements.discs >= 13 &&
            //                requirements.hasLeftConfinesOfWorldWhileFightingEnderDragon &&
            requirements.finishedAdvancements >= 1179 &&
            requirements.obsidianMined >= 1500 &&
            requirements.dragonEggs >= 5 &&
            requirements.levels >= 250 &&
            requirements.duelsWins >= 300
    }

    override suspend fun consumeQuestItems(player: Player): Boolean {
        val requiredShards = 5000L * 9
        val withdrawResult =
            QuestsOG.diamondBankAPI.consumeFromPlayer(
                player.uniqueId,
                requiredShards,
                "Home six quest claimed by ${player.name} (${player.uniqueId})",
                "Quests-OG /claimquest",
            )
        if (withdrawResult.isFailure) return false

        return runOnMainThread {
            player.level -= 250

            val removed = mutableSetOf<Material>()

            for (itemStack in player.inventory.filterNotNull()) {
                if (itemStack.type in neededDiscs && itemStack.type !in removed) {
                    itemStack.amount -= 1
                    removed += itemStack.type
                }
            }

            val notRemoved = player.inventory.removeItem(ItemStack(Material.DRAGON_EGG, 5))

            notRemoved.isEmpty()
        }
    }

    override fun reward(player: Player) {
        val homesFiveNode = PermissionNode.builder("essentials.sethome.multiple.homes-5").build()
        val homesSixNode = PermissionNode.builder("essentials.sethome.multiple.homes-6").build()

        QuestsOG.luckPerms.userManager.modifyUser(player.uniqueId) { user ->
            user.data().remove(homesFiveNode)
            user.data().add(homesSixNode)
        }
    }

    override suspend fun getRequirements(player: Player): Array<Requirement>? {
        val requirements = fetchRequirements(player)

        if (requirements == null) {
            return null
        }

        return arrayOf(
            ProgressRequirement("Total Shards", requirements.totalShards, 5000L * 9),
            ProgressRequirement("Days Played", (requirements.ticksPlayed / 1_728_000).toLong(), 30L),
            ProgressRequirement("Blocks Walked on Water", (requirements.walkOnWaterOneCm / 100).toLong(), 10_000L),
            ProgressRequirement(
                "Blocks Walked Under Water",
                (requirements.walkUnderWaterOneCm / 100).toLong(),
                10_000L,
            ),
            ProgressRequirement("Music Discs", (requirements.discs).toLong(), (13).toLong()),
            ProgressRequirement("Finished Advancements", (requirements.finishedAdvancements).toLong(), (1179).toLong()),
            ProgressRequirement("Obsidian Mined", (requirements.obsidianMined).toLong(), (1500).toLong()),
            ProgressRequirement("Dragon Eggs", (requirements.dragonEggs).toLong(), (5).toLong()),
            ProgressRequirement("Levels", (requirements.levels).toLong(), (250).toLong()),
            ProgressRequirement("Duels Wins", (requirements.duelsWins).toLong(), (300).toLong()),
        )
    }
}
