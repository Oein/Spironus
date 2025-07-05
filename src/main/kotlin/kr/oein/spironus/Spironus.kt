package kr.oein.spironus

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.CommandExecutor
import kr.oein.interchest.GUIListener
import kr.oein.interchest.GUIManager
import kr.oein.spironus.components.Chat
import kr.oein.spironus.components.KVDB
import kr.oein.spironus.components.Random
import kr.oein.spironus.components.Sinsang
import kr.oein.spironus.components.SinsangListener
import kr.oein.spironus.components.SinsangManager
import kr.oein.spironus.components.Whitelist
import kr.oein.spironus.web.WebServer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import kotlin.math.floor


class Spironus : JavaPlugin() {
    var kvdb = KVDB(this)

    val guiManager = GUIManager()
    val guiListener = GUIListener(guiManager)

    val sinsangManager = SinsangManager(this)

    val webServer = WebServer(this)
    
    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(guiListener, this)
        Bukkit.getPluginManager().registerEvents(Whitelist(this), this)
        Bukkit.getPluginManager().registerEvents(Chat(this), this)
        Bukkit.getPluginManager().registerEvents(SinsangListener(this), this)

        saveDefaultConfig()
        config.get("listen") ?.let {
            if (it is Number && it.toInt() > 0) {
                logger.info("Listening on port: ${it.toInt()}")
                webServer.start(it.toInt())
            } else logger.warning("Invalid 'listen' configuration, expected a number but got: $it")
        } ?: run {
            logger.warning("No 'listen(int, 1 - 65535)' configuration found, please check your config.yml")
        }

    }

    override fun onLoad() {
        CommandAPICommand("_0xad312")
            .withSubcommands(
                CommandAPICommand("sinsang")
                    .executes(CommandExecutor { sender, _ ->
                        if(sender !is Player) {
                            sender.sendMessage(Component.text("You can't use this command!", NamedTextColor.RED))
                            return@CommandExecutor
                        }

                        val player = sender
                        if(!player.isOp) {
                            player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.DARK_RED))
                            return@CommandExecutor
                        }

                        val loc = player.location
                        player.teleport(loc.add(0.0, 3.0, 0.0))

                        loc.setRotation(0f, 0f)
                        loc.x = floor(loc.x)
                        loc.y = floor(loc.y)
                        loc.z = floor(loc.z)
                        val world = player.world

                        val shulkers: MutableList<LivingEntity> = mutableListOf()

                        val dx = intArrayOf(-1, 0, 1)
                        val dy = intArrayOf(0, 1, 2)
                        val dz = intArrayOf(-1, 0, 1)

                        val sinsangUUID_Key = NamespacedKey("spironus", "ss_uuid")
                        val sinsangX_Key = NamespacedKey("spironus", "ss_x")
                        val sinsangY_Key = NamespacedKey("spironus", "ss_y")
                        val sinsangZ_Key = NamespacedKey("spironus", "ss_z")

                        val sinsangUUID = Random().generate()

                        for (x in dx) {
                            for (y in dy) {
                                for (z in dz) {
                                    // Spawn shulkers in a 3x3x3 area around the player
                                    val newLoc = loc.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                                    val shulker = world.spawnEntity(newLoc, EntityType.SHULKER)
                                    shulker.isCustomNameVisible = false

                                    val lentity = shulker as LivingEntity
                                    lentity.setAI(false)
                                    lentity.isSilent = true
                                    lentity.isCustomNameVisible = false
                                    lentity.setGravity(false)
                                    lentity.isInvisible = true

                                    lentity.persistentDataContainer.set(
                                        sinsangUUID_Key,
                                        PersistentDataType.STRING,
                                        sinsangUUID
                                    )
                                    lentity.persistentDataContainer.set(
                                        sinsangX_Key,
                                        PersistentDataType.INTEGER,
                                        x + 1
                                    )
                                    lentity.persistentDataContainer.set(
                                        sinsangY_Key,
                                        PersistentDataType.INTEGER,
                                        y
                                    )
                                    lentity.persistentDataContainer.set(
                                        sinsangZ_Key,
                                        PersistentDataType.INTEGER,
                                        z
                                    )

                                    shulkers.add(shulker)
                                }
                            }
                        }

                        loc.add(-0.9, 0.1, -0.9)
                        val blockDisplay = world.spawnEntity(loc, EntityType.BLOCK_DISPLAY) as BlockDisplay
                        blockDisplay.block = Material.IRON_BLOCK.createBlockData()

                        blockDisplay.transformation = Transformation(
                            Vector3f(),
                            AxisAngle4f(),
                            Vector3f(2.8f, 2.8f, 2.8f),
                            AxisAngle4f()
                        )

                        val sinsang = Sinsang(sinsangUUID, this)
                        sinsang.blockDisplay = blockDisplay
                        sinsang.shulkers = shulkers

                        sinsangManager.add(sinsang)
                    })
            )
            .register()
    }

    override fun onDisable() {
        webServer.stop()
    }
}
