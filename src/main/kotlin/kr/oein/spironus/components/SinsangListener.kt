package kr.oein.spironus.components

import kr.oein.spironus.Spironus
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.persistence.PersistentDataType

class SinsangListener(val spironus: Spironus): Listener {
    val sinSnagUUIDKey = NamespacedKey("spironus", "ss_uuid")

    @EventHandler
    fun onEntityHurt(event: EntityDamageEvent) {
        if(event.entity.persistentDataContainer.has(sinSnagUUIDKey)) {
            val len = event.entity as LivingEntity
            val sinsangUUID = event.entity.persistentDataContainer.get(sinSnagUUIDKey, PersistentDataType.STRING)
            if (sinsangUUID == null) return
            len.heal(event.damage)

            if(event.cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return
            spironus.server.broadcast(Component.text("[신상] ${sinsangUUID}이(가) ${event.cause.toString()}로 인해 ${event.damage}의 피해를 입었습니다."))
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
}