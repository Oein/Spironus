package kr.oein.interchest

import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent


interface InventoryHandler {
    fun onClose(event: InventoryCloseEvent?)
    fun onClick(event: InventoryClickEvent)
    fun onOpen(event: InventoryOpenEvent)
}