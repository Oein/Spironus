package kr.oein.interchest

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.Inventory


class GUIManager {
    private val activeInventories: MutableMap<Inventory?, InventoryHandler?> = HashMap<Inventory?, InventoryHandler?>()

    fun openGUI(gui: InventoryGUI, player: Player) {
        this.registerHandledInventory(gui.inventory, gui)
        player.openInventory(gui.inventory)
    }

    fun registerHandledInventory(inventory: Inventory?, handler: InventoryHandler?) {
        this.activeInventories.put(inventory, handler)
    }

    fun unregisterInventory(inventory: Inventory?) {
        this.activeInventories.remove(inventory)
    }

    fun handleClick(event: InventoryClickEvent) {
        val handler = this.activeInventories[event.inventory]
        handler?.onClick(event)
    }

    fun handleOpen(event: InventoryOpenEvent) {
        val handler = this.activeInventories[event.inventory]
        handler?.onOpen(event)
    }

    fun handleClose(event: InventoryCloseEvent) {
        val inventory = event.inventory
        val handler = this.activeInventories[inventory]
        if (handler != null) {
            handler.onClose(event)
            this.unregisterInventory(inventory)
        }
    }
}