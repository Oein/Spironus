package kr.oein.interchest

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.function.Consumer
import java.util.function.Function


class InventoryButton {
    var iconCreator: Function<Player?, ItemStack?>? = null
        private set
    var eventConsumer: Consumer<InventoryClickEvent?>? = null
        private set

    fun creator(iconCreator: Function<Player?, ItemStack?>?): InventoryButton {
        this.iconCreator = iconCreator
        return this
    }

    fun consumer(eventConsumer: Consumer<InventoryClickEvent?>?): InventoryButton {
        this.eventConsumer = eventConsumer
        return this
    }
}