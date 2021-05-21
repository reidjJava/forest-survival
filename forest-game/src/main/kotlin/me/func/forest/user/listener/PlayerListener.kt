package me.func.forest.user.listener

import clepto.bukkit.B
import me.func.forest.app
import me.func.forest.channel.ModTransfer
import me.func.forest.user.listener.prepare.ModLoader
import me.func.forest.user.listener.prepare.PrepareUser
import me.func.forest.user.listener.prepare.SetupScoreBoard
import me.func.forest.user.listener.prepare.TutorialLoader
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED
import org.bukkit.event.player.PlayerRespawnEvent
import kotlin.math.min


/**
 * Временный класс
 */
class PlayerListener : Listener {

    private val textureUrl = System.getenv("RESOURCE_PACK_URL")
    private val textureHash = "08880C088F83D8890128129"

    private val prepares = listOf(
        ModLoader(),
        TutorialLoader(),
        PrepareUser { it.player.setResourcePack(textureUrl, textureHash) },
        PrepareUser { it.player.gameMode = GameMode.SURVIVAL },
        SetupScoreBoard()
    )

    @EventHandler
    fun PlayerJoinEvent.handle() {
        prepares.forEach { it.execute(app.getUser(player)!!) }
    }

    @EventHandler
    fun PlayerResourcePackStatusEvent.handle() {
        if (status == SUCCESSFULLY_LOADED) {
            B.postpone(20) { ModTransfer().send("guide", app.getUser(player)!!) }
        }
    }

    @EventHandler
    fun FoodLevelChangeEvent.handle() {
        if (entity is CraftPlayer)
            ModTransfer().integer(min(20, foodLevel)).send("food-level", app.getUser(entity as CraftPlayer)!!)
    }

    @EventHandler
    fun PlayerRespawnEvent.handle() {
        val place = app.getUser(player)!!.stat!!.place
        respawnLocation = if (place == null)
            app.worldMeta.getLabel("guide_end")
        else
            Location(app.getWorld(), place.x, place.y, place.z)
    }

    @EventHandler
    fun EntityDamageByEntityEvent.handle() {
        if (entity !is CraftPlayer)
            return

        val damageBy = if (damager is CraftPlayer)
            damager as CraftPlayer
        else if (damager is Projectile && (damager as Projectile).shooter is CraftPlayer)
            (damager as Projectile).shooter as CraftPlayer
        else null

        if (damageBy != null) {
            val entityLvl = app.getUser(entity as CraftPlayer)!!.level
            val damagerLvl = app.getUser(damageBy)!!.level

            if (entityLvl != damagerLvl || entityLvl < 3 || damagerLvl < 3)
                cancelled = true
        }
    }

    @EventHandler
    fun PlayerDeathEvent.handle() {
        val player = getEntity()
        val user = app.getUser(player)!!
        val stat = user.stat!!
        stat.heart--

        player.activePotionEffects.forEach {
            player.removePotionEffect(it.type)
        }

        if (stat.heart < 1) {
            stat.exp = 0
            user.giveExperience(0)
            stat.heart = 3
            stat.timeAlive = 0
            stat.temperature = 36.6
            stat.place = null
            stat.placeInventory.clear()
        } else {
            cancelled = true
            user.player.health = 20.0
            user.player.teleport(app.worldMeta.getLabel("guide_end"))
            ModTransfer()
                .integer(stat.heart + 1)
                .send("player-dead", user)
        }
    }
}