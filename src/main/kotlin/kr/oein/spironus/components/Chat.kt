package kr.oein.spironus.components

import io.papermc.paper.event.player.AsyncChatEvent
import kr.oein.spironus.Spironus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import javax.naming.Name

class Chat(val spironus: Spironus): Listener {
    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        event.isCancelled = true

        val player = event.player
        val uuid = player.uniqueId.toString()
        val crcid = spironus.kvdb.get("playerinfo-uuid2crc", uuid)
        if(crcid is String) {
            val senderTeam = spironus.kvdb.get("playerinfo-team", crcid)
            if (senderTeam is String && senderTeam != "-1") {
                val senderTeamMaster = spironus.kvdb.loadScope("teams").yamlcfg.getConfigurationSection(senderTeam)
                    ?.getString("masterPid")
                val isMaster = senderTeamMaster == crcid
                spironus.server.onlinePlayers.forEach { player ->
                    val playerCrc = spironus.kvdb.get("playerinfo-uuid2crc", player.uniqueId.toString())
                    if (playerCrc is String) {
                        val playerTeam = spironus.kvdb.get("playerinfo-team", playerCrc)
                        if (playerTeam is String) {
                            if (playerTeam == senderTeam) {
                                player.sendMessage(
                                    Component.text("<")
                                        .append { Component.text(if(isMaster) "👑 " else "", NamedTextColor.YELLOW) }
                                        .append { Component.text("${event.player.name}> ", NamedTextColor.WHITE) }
                                        .append { event.message() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}