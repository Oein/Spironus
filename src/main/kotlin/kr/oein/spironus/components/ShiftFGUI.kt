package kr.oein.spironus.components

import kr.oein.interchest.InventoryButton
import kr.oein.interchest.InventoryGUI
import kr.oein.spironus.Spironus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class ShiftFGUI(val spironus: Spironus): InventoryGUI() {
    val cooldownSecs = 60L
    private fun getNowTime(): Long {
        val now = System.currentTimeMillis()
        return now
    }
    override fun createInventory(): Inventory {
        return Bukkit.createInventory(null, 3 * 9, "Shift-F GUI")
    }

    override fun decorate(player: Player?) {
        if (player == null) return
        val sinsangKeys = spironus.kvdb.keys("sinsang")
        var i = 0
        var cooldown = spironus.kvdb.loadScope("cooldown").yamlcfg.getConfigurationSection(player.uniqueId.toString())
        if (cooldown == null) {
            cooldown = spironus.kvdb.loadScope("cooldown").yamlcfg.createSection(player.uniqueId.toString())
        }
        for (key in sinsangKeys) {
            val item = spironus.kvdb.loadScope("sinsang").yamlcfg.getConfigurationSection(key)
            if (item == null) continue
            val sinsangName = item.getString("name") ?: "Unknown Sinsang"
            val sinsangX = item.getInt("locX", 0) + 3
            val sinsangY = item.getInt("locY", 0)
            val sinsangZ = item.getInt("locZ", 0)
            val sinsangWorlds = spironus.server.worlds.take(1)
            if(sinsangWorlds.isEmpty()) continue
            val sinsangWorld = sinsangWorlds[0]
            var loc = Location(sinsangWorld, sinsangX.toDouble(), sinsangY.toDouble(), sinsangZ.toDouble())
            loc = sinsangWorld.getHighestBlockAt(loc).location
            loc = loc.add(0.0, 1.0, 0.0)

            val cooldownData = cooldown.getLong(key, 0L)
            val currentTime = getNowTime()
            val flowedTime = if (cooldownData == 0L) 0L else currentTime - cooldownData
            val canUse = if(flowedTime == 0L) true else flowedTime >= cooldownSecs * 1000L // 60 seconds cooldown

            this.addButton(
                i++,
                InventoryButton()
                    .creator { it ->
                        val item = ItemStack(
                            if(canUse) Material.GREEN_STAINED_GLASS_PANE
                            else Material.CLOCK
                        )
                        item.editMeta { meta ->

                            meta.displayName(Component.text(sinsangName, NamedTextColor.GREEN))
                            meta.lore(
                                listOf(
                                    Component.text("위치: ${sinsangWorld.name} (${sinsangX}, ${sinsangY}, ${sinsangZ})", NamedTextColor.GRAY),
                                    if(canUse)
                                        Component.text("클릭하여 이동", NamedTextColor.GRAY)
                                    else
                                        Component.text("쿨타임: ${(cooldownSecs - flowedTime / 1000).toInt()}초", NamedTextColor.RED)
                                )
                            )
                        }
                        item
                    }
                    .consumer { event ->
                        val cooldownData = cooldown.getLong(key, 0L)
                        val currentTime = getNowTime()
                        val flowedTime = if (cooldownData == 0L) 0L else currentTime - cooldownData
                        val canUse =
                            if (flowedTime == 0L) true else flowedTime >= cooldownSecs * 1000L // 60 seconds cooldown

                        event?.inventory?.close()
                        if (canUse) {
                            event?.whoClicked?.teleport(loc)
                            cooldown.set(key, currentTime) // Update cooldown time
                        } else event?.whoClicked?.sendMessage(
                                Component.text("쿨타임이 끝나지 않았습니다. 잠시 후 다시 시도해주세요.", NamedTextColor.RED)
                            )
                    }
            )
        }

        super.decorate(player)
    }
}