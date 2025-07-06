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
import org.bukkit.attribute.Attribute
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Slime
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import kotlin.math.floor

class SinsangManageCommand(val spironus: Spironus) {
    val sinsangManager = spironus.sinsangManager
    val sinsangSize = 4

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
                                player.teleport(player.location.add(0.0, sinsangSize.toDouble() + 1.0, 0.0))

                                loc.setRotation(0f, 0f)
                                loc.x = floor(loc.x)
                                loc.y = floor(loc.y)
                                loc.z = floor(loc.z)
                                val origLoc = loc.clone()
                                val world = player.world

                                val sinsangUUID_Key = NamespacedKey("spironus", "ss_uuid")

                                val sinsangUUID = Random().generate()

                                // Spawn shulkers in a 3x3x3 area around the player
                                val slime = world.spawnEntity(loc, EntityType.SLIME)
                                slime.isCustomNameVisible = false

                                val lentity = slime as Slime
                                lentity.setAI(false)
                                lentity.isSilent = true
                                lentity.isCustomNameVisible = false
                                lentity.setGravity(false)
                                lentity.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0))

                                lentity.size = (sinsangSize * 2)
                                lentity.registerAttribute(Attribute.MAX_HEALTH)
                                lentity.getAttribute(Attribute.MAX_HEALTH)?.let { it.baseValue = 2048.0 }
                                lentity.health = 2048.0

                                lentity.persistentDataContainer.set(
                                    sinsangUUID_Key,
                                    PersistentDataType.STRING,
                                    sinsangUUID
                                )

                                loc.add(-floor(sinsangSize.toDouble() / 2) + 0.1, 0.1, -floor(sinsangSize.toDouble() / 2) + 0.1)
                                val blockDisplay = world.spawnEntity(loc, EntityType.BLOCK_DISPLAY) as BlockDisplay
                                blockDisplay.block = Material.WHITE_CONCRETE.createBlockData()

                                blockDisplay.transformation = Transformation(
                                    Vector3f(),
                                    AxisAngle4f(),
                                    Vector3f(sinsangSize.toFloat() - 0.2f, sinsangSize.toFloat() - 0.2f, sinsangSize.toFloat() - 0.2f),
                                    AxisAngle4f()
                                )
                                blockDisplay.persistentDataContainer.set(
                                    sinsangUUID_Key,
                                    PersistentDataType.STRING,
                                    sinsangUUID
                                )

                                val sinsang = Sinsang(sinsangUUID, spironus)
                                sinsang.blockDisplay = blockDisplay
                                sinsang.shulkers = listOf(slime as LivingEntity).toMutableList()
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
                            }),
                        CommandAPICommand("deleteAll")
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

                                sinsangManager.sinsangs.forEach { s ->
                                    s.component2().destroy()
                                }
                                sinsangManager.sinsangs.clear()
                                player.sendMessage(Component.text("All sinsangs have been deleted.", NamedTextColor.GREEN))

                                // loop in all entity
                                for(entity in player.world.entities) {
                                    if (entity is Slime && entity.persistentDataContainer.has(NamespacedKey("spironus", "ss_uuid"))) {
                                        entity.remove()
                                    } else if (entity is BlockDisplay && entity.persistentDataContainer.has(NamespacedKey("spironus", "ss_uuid"))) {
                                        entity.remove()
                                    }
                                }
                            })
                    )
            )
            .register()
    }
}