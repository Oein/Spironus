package kr.oein.spironus.components

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.CommandExecutor
import kr.oein.spironus.Spironus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException


class TeamChest(val spironus: Spironus): Listener {
    /**
     * Converts the player inventory to a String array of Base64 strings. First string is the content and second string is the armor.
     *
     * @param playerInventory to turn into an array of strings.
     * @return Array of strings: [ main content, armor content ]
     * @throws IllegalStateException
     */
    @Throws(IllegalStateException::class)
    fun playerInventoryToBase64(playerInventory: PlayerInventory): Array<String> {
        //get the main content part, this doesn't return the armor
        val content = toBase64(playerInventory)
        val armor = itemStackArrayToBase64(playerInventory.armorContents)

        return arrayOf(content, armor)
    }

    /**
     *
     * A method to serialize an [ItemStack] array to Base64 String.
     * Based off of [.toBase64].
     *
     * @param items to turn into a Base64 String.
     * @return Base64 string of the items.
     * @throws IllegalStateException
     */
    @Throws(IllegalStateException::class)
    fun itemStackArrayToBase64(items: Array<ItemStack?>): String {
        try {
            val outputStream = ByteArrayOutputStream()
            val dataOutput = BukkitObjectOutputStream(outputStream)

            // Write the size of the inventory
            dataOutput.writeInt(items.size)

            // Save every element in the list
            for (i in items.indices) {
                dataOutput.writeObject(items[i])
            }

            // Serialize that array
            dataOutput.close()
            return Base64Coder.encodeLines(outputStream.toByteArray())
        } catch (e: Exception) {
            throw IllegalStateException("Unable to save item stacks.", e)
        }
    }

    /**
     * A method to serialize an inventory to Base64 string.
     *
     * Special thanks to Comphenix in the Bukkit forums or also known
     * as aadnk on GitHub.
     *
     * [Original Source](https://gist.github.com/aadnk/8138186)
     *
     * @param inventory to serialize
     * @return Base64 string of the provided inventory
     * @throws IllegalStateException
     */
    @Throws(IllegalStateException::class)
    fun toBase64(inventory: Inventory): String {
        try {
            val outputStream = ByteArrayOutputStream()
            val dataOutput = BukkitObjectOutputStream(outputStream)


            // Write the size of the inventory
            dataOutput.writeInt(inventory.size)


            // Save every element in the list
            for (i in 0..<inventory.size) {
                dataOutput.writeObject(inventory.getItem(i))
            }


            // Serialize that array
            dataOutput.close()
            return Base64Coder.encodeLines(outputStream.toByteArray())
        } catch (e: Exception) {
            throw IllegalStateException("Unable to save item stacks.", e)
        }
    }

    /**
     *
     * A method to get an [Inventory] from an encoded, Base64, string.
     * Special thanks to Comphenix in the Bukkit forums or also known
     * as aadnk on GitHub.
     *
     * [Original Source](https://gist.github.com/aadnk/8138186)
     *
     * @param data Base64 string of data containing an inventory.
     * @return Inventory created from the Base64 string.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun fromBase64(data: String): Inventory {
        try {
            val inputStream = ByteArrayInputStream(Base64Coder.decodeLines(data))
            val dataInput = BukkitObjectInputStream(inputStream)
            val inventory: Inventory = Bukkit.getServer().createInventory(null, dataInput.readInt())


            // Read the serialized inventory
            for (i in 0..<inventory.size) {
                inventory.setItem(i, dataInput.readObject() as ItemStack?)
            }

            dataInput.close()
            return inventory
        } catch (e: ClassNotFoundException) {
            throw IOException("Unable to decode class type.", e)
        }
    }

    /**
     * Gets an array of ItemStacks from Base64 string.
     *
     * Base off of [.fromBase64].
     *
     * @param data Base64 string to convert to ItemStack array.
     * @return ItemStack array created from the Base64 string.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun itemStackArrayFromBase64(data: String): Array<ItemStack?> {
        try {
            val inputStream = ByteArrayInputStream(Base64Coder.decodeLines(data))
            val dataInput = BukkitObjectInputStream(inputStream)
            val items = arrayOfNulls<ItemStack>(dataInput.readInt())


            // Read the serialized inventory
            for (i in items.indices) {
                items[i] = dataInput.readObject() as ItemStack?
            }

            dataInput.close()
            return items
        } catch (e: ClassNotFoundException) {
            throw IOException("Unable to decode class type.", e)
        }
    }

    val teamChests: MutableMap<String, Inventory> = mutableMapOf()

    fun registerCommands() {
        CommandAPICommand("teamchest")
            .executes(CommandExecutor { sender, _ ->
                if(sender !is Player) {
                    sender.sendMessage("This command can only be used by players.")
                    return@CommandExecutor
                }

                val player = sender as Player
                val crc = spironus.kvdb.get("playerinfo-uuid2crc", player.uniqueId.toString())
                if(crc !is String) {
                    player.sendMessage("You are not registered in the player info database.")
                    return@CommandExecutor
                }
                val team = spironus.kvdb.get("playerinfo-team", crc)
                if(team !is String || team == "-1") {
                    player.sendMessage(
                        Component.text(
                            "현재 아무 팀에도 소속되어 있지 않습니다. 팀에 가입한 후 다시 시도해주세요.",
                            NamedTextColor.RED
                        )
                    )
                    return@CommandExecutor
                }

                val teamChest = teamChests[team]
                if(teamChest == null) {
                    val teamChestData = spironus.kvdb.get("teamchest", team)
                    if(teamChestData is String) {
                        try {
                            val inventory = fromBase64(teamChestData)
                            teamChests[team] = inventory
                        } catch (e: IOException) {
                            player.sendMessage(
                                Component.text(
                                    "팀 상자 데이터를 불러오는 데 실패했습니다. 관리자에게 문의하세요.",
                                    NamedTextColor.RED
                                )
                            )
                            return@CommandExecutor
                        }
                    } else {
                        teamChests[team] = Bukkit.createInventory(null, 54)
                    }
                }

                // show the team chest inventory to the player
                player.openInventory(teamChests[team]!!)
            })
            .register()
    }

    @EventHandler
    fun onChestClose(event: InventoryCloseEvent) {
        val inventory = event.inventory
        if (inventory.type != org.bukkit.event.inventory.InventoryType.CHEST) return

        for(teamChestKV in teamChests) {
            val team = teamChestKV.key
            val teamChest = teamChestKV.value

            if(teamChest != inventory) continue
            // Save the team chest inventory to the database
            try {
                val base64 = toBase64(inventory)
                spironus.kvdb.set("teamchest", team, base64)
            } catch (e: IllegalStateException) {
                event.player.sendMessage(
                    Component.text(
                        "팀 상자 데이터를 저장하는 데 실패했습니다. 관리자에게 문의하세요.",
                        NamedTextColor.RED
                    )
                )
                return
            }
        }
    }
}