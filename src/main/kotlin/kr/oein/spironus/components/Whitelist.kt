package kr.oein.spironus.components

import kr.oein.spironus.Spironus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.util.zip.CRC32

class Whitelist(val spironus: Spironus) : Listener {
    val kv = spironus.kvdb
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val playerStr = player.uniqueId.toString()
        val crcid = CRC32().apply {
            update(playerStr.toByteArray())
        }.value.toString(36)
        if (!kv.has("whitelist", crcid)) {
            player.kick(
                Component.text("등록되지 않은 플레이어 입니다.\nPID : ")
                    .append { Component.text("$crcid\n", NamedTextColor.YELLOW) }
                    .append { Component.text("위 ID를 국가의 권력가 혹은 관리자에게 제출하여 서버 입장 허가를 받으세요.\n") }
                    .append { Component.text("위 코드가 노출될 경우, 서버 데이터 삭제, 원치않는 국가 소속등 불상사가 있을 수 있으니 주의하세요.", NamedTextColor.RED) }
            )
        }

        if(!kv.has("playerinfo-crc2uuid", crcid))
            kv.set("playerinfo-crc2uuid", crcid, player.uniqueId.toString())
        if(!kv.has("playerinfo-uuid2crc", player.uniqueId.toString()))
            kv.set("playerinfo-uuid2crc", player.uniqueId.toString(), crcid)
        if(!kv.has("playerinfo-team", crcid))
            kv.set("playerinfo-team", crcid, "-1")

        kv.set("playerinfo-nickname", crcid, player.name)
    }
}