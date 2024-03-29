package me.reidj.forest

import clepto.bukkit.B
import clepto.cristalix.WorldMeta
import dev.implario.bukkit.platform.Platforms
import dev.implario.platform.impl.darkpaper.PlatformDarkPaper
import me.func.mod.Anime
import me.func.mod.Kit
import me.func.mod.conversation.ModLoader
import me.func.mod.util.listener
import me.reidj.forest.channel.item.ItemManager
import me.reidj.forest.clock.GameTimer
import me.reidj.forest.command.PlayerCommands
import me.reidj.forest.craft.CraftManager
import me.reidj.forest.drop.ResourceManager
import me.reidj.forest.effect.EffectManager
import me.reidj.forest.listener.CancelEvents
import me.reidj.forest.listener.DamageHandler
import me.reidj.forest.listener.JoinEvent
import me.reidj.forest.listener.TentManipulator
import me.reidj.forest.user.User
import me.reidj.forest.util.MapLoader
import me.reidj.forest.water.WaterManager
import me.reidj.forest.weather.ZoneManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.cristalix.core.CoreApi
import ru.cristalix.core.inventory.IInventoryService
import ru.cristalix.core.inventory.InventoryService
import ru.cristalix.core.network.ISocketClient
import ru.cristalix.core.realm.IRealmService
import ru.cristalix.core.realm.RealmId
import ru.cristalix.core.realm.RealmStatus
import ru.cristalix.core.scoreboard.IScoreboardService
import ru.cristalix.core.scoreboard.ScoreboardService
import java.util.*

private const val PROJECT_NAME = "Лес"

lateinit var app: App

val clientSocket: ISocketClient = ISocketClient.get()

class App : JavaPlugin() {

    lateinit var worldMeta: WorldMeta

    lateinit var spawn: Location
    lateinit var start: Location

    val userMap = mutableMapOf<UUID, User>()

    private lateinit var waterManager: WaterManager

    override fun onEnable() {
        app = this
        B.plugin = this

        Platforms.set(PlatformDarkPaper())

        // Регистрация Core сервисов
        CoreApi.get().run {
            registerService(IInventoryService::class.java, InventoryService())
            registerService(IScoreboardService::class.java, ScoreboardService())
        }

        Anime.include(Kit.STANDARD, Kit.NPC, Kit.DEBUG)

        ModLoader.loadAll("mods")

        // Загрузка карты
        worldMeta = MapLoader().load("prod")!!
        spawn = worldMeta.getLabel("guide_end")
        start = worldMeta.getLabel("guide_pre")

        // Конфигурация реалма
        val info = IRealmService.get().currentRealmInfo
        info.status = RealmStatus.WAITING_FOR_PLAYERS
        info.groupName = PROJECT_NAME
        IScoreboardService.get().serverStatusBoard.displayName = "§fЛес"

        // Регистрация команд
        PlayerCommands

        // Регистрация меню крафтов
        CraftManager()

        waterManager = WaterManager()

        // Регистрация обработчиков событий
        listener(CancelEvents(), DamageHandler(), ItemManager(), ResourceManager(), JoinEvent(), TentManipulator(), waterManager)

        // Начало игрового времени и добавление временных собитий
        Bukkit.getScheduler().runTaskTimerAsynchronously(
            this, GameTimer(
                listOf(
                    ZoneManager(),
                    EffectManager(),
                    waterManager
                )
            ), 0, 1
        )
    }

    override fun onDisable() {
        Bukkit.getOnlinePlayers()
            .map { getUser(it) }
            .forEach {
                val dot = it!!.player!!.location

                it.ifTent { _ ->
                    it.saveInventory(it.stat.tentInventory, it.tentInventory)
                    it.tent!!.remove()
                }
                it.saveInventory(it.stat.playerInventory, it.player!!.inventory)

                it.stat.exit = ru.cristalix.core.math.V3(dot.x, dot.y, dot.z)
            }
    }

    fun getUser(uuid: UUID): User? = userMap[uuid]

    fun getUser(player: Player): User? = getUser(player.uniqueId)

    fun getWorld(): World = worldMeta.world

    fun getHub(): RealmId = RealmId.of("HUB-2")
}