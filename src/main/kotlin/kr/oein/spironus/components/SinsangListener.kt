package kr.oein.spironus.components

import kr.oein.spironus.Spironus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.persistence.PersistentDataType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SinsangListener(val spironus: Spironus): Listener {
    val sinSnagUUIDKey = NamespacedKey("spironus", "ss_uuid")

    val jumryungStartAt = 12 + 7
    val jumryungEndAt = 12 + 10

    @EventHandler
    fun onEntityHurt(event: EntityDamageEvent) {
        if(event.entity.persistentDataContainer.has(sinSnagUUIDKey)) {
            val len = event.entity as LivingEntity
            val sinsangUUID = event.entity.persistentDataContainer.get(sinSnagUUIDKey, PersistentDataType.STRING)
            if (sinsangUUID == null) return
            for(a in 0..200) len.heal(10.0)

            val currentDateTime = LocalDateTime.now().hour
            val isJumryungTime = currentDateTime in jumryungStartAt..jumryungEndAt

            if(event.cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK && event.cause != EntityDamageEvent.DamageCause.PROJECTILE && event.cause != EntityDamageEvent.DamageCause.FIRE_TICK) return
            val hitBy = event.damageSource
            if (event.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK && hitBy.causingEntity is Player) {
                val player = hitBy.causingEntity as Player
                val playerUUID = player.uniqueId.toString()
                val crcID = spironus.kvdb.get("playerinfo-uuid2crc", playerUUID)
                if(crcID is String) {
                    val team = spironus.kvdb.get("playerinfo-team", crcID)
                    if(team == "-1") {
                        event.isCancelled = true
                        player.sendMessage { Component.text("무소속 상태로는 신상을 점령할 수 없습니다.", NamedTextColor.RED) }
                    }
                    else if(!isJumryungTime) {
                        event.isCancelled = true
                        player.sendMessage { Component.text("점령 시간은 ${jumryungStartAt}시 부터 ${jumryungEndAt}시 사이 입니다.", NamedTextColor.RED) }
                    }
                    else if(team is String) {
                        spironus.sinsangManager.setLastHitterName(sinsangUUID, player.name)
                        spironus.sinsangManager.damage(sinsangUUID, event.damage, team)
                    }
                }
                for(a in 0..200) len.heal(10.0)
                return
            }

            for(a in 0..200) len.heal(10.0)
            if(isJumryungTime)
                spironus.sinsangManager.damage(sinsangUUID, event.damage)
        }
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        if(event.entity.persistentDataContainer.has(sinSnagUUIDKey)) {
            val sinsangUUID = event.entity.persistentDataContainer.get(sinSnagUUIDKey, PersistentDataType.STRING)
            if (sinsangUUID == null) return
            spironus.server.broadcast(Component.text("${event.entity.name}이(가) 죽었습니다."))
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.player
        val crc = spironus.kvdb.get("playerinfo-uuid2crc", player.uniqueId.toString())
        if(crc is String) {
            val team = spironus.kvdb.get("playerinfo-team", crc)
            if(team is String && team != "-1") {
                var hasSinsang = false
                for(sinsang in spironus.sinsangManager.sinsangs.values) {
                    if (sinsang.owner == team) hasSinsang = true
                }

                if(!hasSinsang) {
                    player.sendMessage { Component.text("점령하고 있는 신상이 없어 팀에서 나가졌습니다.", NamedTextColor.RED) }
                    spironus.kvdb.set("playerinfo-team", crc, "-1")
                    spironus.server.broadcast(Component.text("${player.name}이(가) 점령하고 있는 신상이 없어 팀에서 나갔습니다.", NamedTextColor.RED))
                }
            }
        }
    }
}