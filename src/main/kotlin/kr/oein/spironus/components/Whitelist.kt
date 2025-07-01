package kr.oein.spironus.components

import kr.oein.spironus.Spironus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.util.zip.CRC32

class Whitelist(val spironus: Spironus) : Listener {
    val kv = KVDB(spironus)
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val playeridStr = player.uniqueId.toString()
        val crcid = CRC32().apply {
            update(playeridStr.toByteArray())
        }.value.toString(36)
        if (!kv.has("whitelist.$crcid")) {
            player.kick(
                Component.text("등록되지 않은 플레이어 입니다.\nPID : ")
                    .append { Component.text("$crcid\n", NamedTextColor.YELLOW) }
                    .append(Component.text("위 ID를 국가의 권력가 혹은 관리자에게 제출하여 서버 입장을 허가받으시오."))
            )
        }
    }
}