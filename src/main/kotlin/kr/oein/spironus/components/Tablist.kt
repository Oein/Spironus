package kr.oein.spironus.components

import kr.oein.spironus.Spironus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class Tablist(val spironus: Spironus) : Listener {
    fun setPlayerPrefix(player: Player, prefix: String) {
        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        val teamName = "tab_${player.name.take(12)}"

        scoreboard.getTeam(teamName)?.unregister()
        val team = scoreboard.registerNewTeam(teamName)

        team.prefix(Component.text(prefix, NamedTextColor.AQUA))
        team.addEntry(player.name)

        player.scoreboard = scoreboard
    }
    private fun updateTablist(player: Player) {
        val playerUUID = player.uniqueId.toString()
        val crcID = spironus.kvdb.get("playerinfo-uuid2crc", playerUUID)
        if( crcID !is String){
            return
        }

        var team = spironus.kvdb.get("playerinfo-team", crcID) ?: "-1"
        if(team !is String) team = team.toString()

        val teamName = spironus.kvdb.loadScope("teams").yamlcfg.getConfigurationSection(team)?.getString("name") ?: "무소속"
        val header =
            Component.text("\n        환영합니다, ", NamedTextColor.WHITE)
                .append {
                    Component.text(player.name, NamedTextColor.GOLD)
                        .style { builder ->
                            builder.decorate(TextDecoration.BOLD)
                                .build()
                        }
                }
                .append { Component.text("님!        \n", NamedTextColor.WHITE) }
        val footer = Component.text("\n        당신의 팀 : [ $teamName ]       \n", NamedTextColor.AQUA)
        player.sendPlayerListHeaderAndFooter(header, footer)
        setPlayerPrefix(player, "[ $teamName ] ")
    }
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        updateTablist(event.player)
    }
    fun schedule() {
        spironus.server.scheduler.scheduleSyncRepeatingTask(spironus, {
            for (player in spironus.server.onlinePlayers) updateTablist(player)
        }, 20, 20)
    }


}
