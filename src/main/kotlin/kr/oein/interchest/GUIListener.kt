package kr.oein.interchest

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent


class GUIListener(private val guiManager: GUIManager) : Listener {
    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        this.guiManager.handleClick(event)
    }

    @EventHandler
    fun onOpen(event: InventoryOpenEvent) {
        this.guiManager.handleOpen(event)
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        this.guiManager.handleClose(event)
    }
}