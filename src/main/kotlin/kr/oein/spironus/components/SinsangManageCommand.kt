package kr.oein.spironus.components

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.ListArgumentBuilder
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import kr.oein.spironus.Spironus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import kotlin.math.floor

class SinsangManageCommand(val spironus: Spironus) {
    val sinsangManager = spironus.sinsangManager

    fun registerCommands() {
        val sinsangListArgu = ListArgumentBuilder<String>("uid")
            .withList { sender ->
                val collection = mutableListOf<String>()
                sinsangManager.sinsangs.keys.forEach { uid ->
                    collection.add(uid)
                }
                collection
            }
            .withMapper { it -> it.toString() }
            .buildGreedy()
        CommandAPICommand("_0xad312")
            .withSubcommands(
                CommandAPICommand("sinsang")
                    .withSubcommands(
                        CommandAPICommand("spawn")
                            .withArguments(StringArgument("name"))
                            .executes(CommandExecutor { sender, args ->
                                if(sender !is Player) {
                                    sender.sendMessage(Component.text("You can't use this command!", NamedTextColor.RED))
                                    return@CommandExecutor
                                }

                                val player = sender
                                if(!player.isOp) {
                                    player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.DARK_RED))
                                    return@CommandExecutor
                                }

                                val name = args.get("name") as String
                                if(name.isBlank()) {
                                    player.sendMessage(Component.text("Name cannot be empty!", NamedTextColor.RED))
                                    return@CommandExecutor
                                }

                                val loc = player.location.clone()
                                player.teleport(player.location.add(0.0, 4.0, 0.0))

                                loc.setRotation(0f, 0f)
                                loc.x = floor(loc.x)
                                loc.y = floor(loc.y)
                                loc.z = floor(loc.z)
                                val origLoc = loc.clone()
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

                                val sinsang = Sinsang(sinsangUUID, spironus)
                                sinsang.blockDisplay = blockDisplay
                                sinsang.shulkers = shulkers
                                sinsang.locX = origLoc.x
                                sinsang.locY = origLoc.y
                                sinsang.locZ = origLoc.z
                                sinsang.name = name

                                sinsang.updateBossbarTitle()

                                sinsangManager.add(sinsang)
                            }),
                        CommandAPICommand("list")
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

                                if(sinsangManager.sinsangs.isEmpty()) {
                                    player.sendMessage(Component.text("No sinsangs found.", NamedTextColor.YELLOW))
                                    return@CommandExecutor
                                }

                                player.sendMessage(Component.text("Sinsangs List:", NamedTextColor.GREEN))
                                sinsangManager.sinsangs.forEach { (uuid, sinsang) ->
                                    player.sendMessage(
                                        Component.text("UUID: $uuid, Location: (${sinsang.locX}, ${sinsang.locY}, ${sinsang.locZ})", NamedTextColor.YELLOW)
                                    )
                                }
                            }),
                        CommandAPICommand("destroy")
                            .withArguments(sinsangListArgu)
                            .executes(CommandExecutor { sender, args ->
                                if(sender !is Player) {
                                    sender.sendMessage(Component.text("You can't use this command!", NamedTextColor.RED))
                                    return@CommandExecutor
                                }

                                val player = sender
                                if(!player.isOp) {
                                    player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.DARK_RED))
                                    return@CommandExecutor
                                }

                                for(uid in args.get("uid") as List<String>) {
                                    val sinsang = sinsangManager.sinsangs[uid]
                                    if (sinsang != null) {
                                        sinsangManager.destroy(uid)
                                        player.sendMessage(
                                            Component.text(
                                                "Sinsang $uid has been destroyed.",
                                                NamedTextColor.GREEN
                                            )
                                        )
                                    } else {
                                        player.sendMessage(
                                            Component.text(
                                                "Sinsang with UID $uid not found.",
                                                NamedTextColor.RED
                                            )
                                        )
                                    }
                                }
                            }),
                        CommandAPICommand("heal")
                            .withArguments(IntegerArgument("amount"))
                            .withArguments(sinsangListArgu)
                            .executes(CommandExecutor { sender, args ->
                                if(sender !is Player) {
                                    sender.sendMessage(Component.text("You can't use this command!", NamedTextColor.RED))
                                    return@CommandExecutor
                                }

                                val player = sender
                                if(!player.isOp) {
                                    player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.DARK_RED))
                                    return@CommandExecutor
                                }

                                val amount = args.get("amount") as Int
                                for(uid in args.get("uid") as List<String>) {
                                    sinsangManager.heal(uid, amount.toDouble())
                                    player.sendMessage(
                                        Component.text("Sinsang $uid has been healed by $amount.", NamedTextColor.GREEN)
                                    )
                                }
                            }),
                        CommandAPICommand("damage")
                            .withArguments(IntegerArgument("amount"))
                            .withArguments(sinsangListArgu)
                            .executes(CommandExecutor { sender, args ->
                                if(sender !is Player) {
                                    sender.sendMessage(Component.text("You can't use this command!", NamedTextColor.RED))
                                    return@CommandExecutor
                                }

                                val player = sender
                                if(!player.isOp) {
                                    player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.DARK_RED))
                                    return@CommandExecutor
                                }

                                val amount = args.get("amount") as Int
                                for(uid in args.get("uid") as List<String>) {
                                    sinsangManager.damage(uid, amount.toDouble())
                                    player.sendMessage(
                                        Component.text("Sinsang $uid has been damaged by $amount.", NamedTextColor.GREEN)
                                    )
                                }
                            })
                    )
            )
            .register()
    }
}