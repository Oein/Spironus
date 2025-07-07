package kr.oein.spironus.components

import kr.oein.spironus.Spironus
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class ShiftF (val spironus: Spironus): Listener {
    @EventHandler
    fun onSwapItem(event: org.bukkit.event.player.PlayerSwapHandItemsEvent) {
        if (event.isCancelled) return
        if (event.player.isSneaking) {
            event.isCancelled = true
            spironus.guiManager.openGUI(ShiftFGUI(), event.player)
        }
    }
}