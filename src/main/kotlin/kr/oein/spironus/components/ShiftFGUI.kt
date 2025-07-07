package kr.oein.spironus.components

import kr.oein.interchest.InventoryButton
import kr.oein.interchest.InventoryGUI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class ShiftFGUI: InventoryGUI() {
    override fun createInventory(): Inventory {
        return Bukkit.createInventory(null, 3 * 9, "Shift-F GUI")
    }

    override fun decorate(player: Player?) {
        this.addButton(
            13,
            InventoryButton()
                .creator { it -> ItemStack(Material.GREEN_STAINED_GLASS) }
                .consumer { event ->
                    event?.whoClicked?.sendMessage { Component.text("Shift-F GUI button clicked!", NamedTextColor.GREEN) }
                    event?.inventory?.close()
                }
        )


        super.decorate(player)
    }
}