package kr.oein.spironus.components

import kr.oein.spironus.Spironus
import org.bukkit.Location
import kotlin.math.pow
import kotlin.math.sqrt

class SinsangManager(val spironus: Spironus) {
    val sinsangs = mutableMapOf<String, Sinsang>()
    val scope = spironus.kvdb.loadScope("sinsang")

    fun load() {
        scope.yamlcfg.getKeys(false).forEach { uuid ->
            val sinsang = Sinsang(uuid, spironus)
            sinsang.load()
            sinsangs[uuid] = sinsang
        }
    }

    fun add(sinsang: Sinsang) {
        sinsangs[sinsang.uuid] = sinsang
        sinsang.save()
    }

    fun damage(uuid: String, amount: Double, team: String) {
        val sinsang = sinsangs[uuid]
        if (sinsang != null) {
            sinsang.damage(amount, team)
        } else {
            spironus.logger.warning("Sinsang with UUID $uuid not found.")
        }
    }

    fun damage(uuid: String, amount: Double) {
        val sinsang = sinsangs[uuid]
        if (sinsang != null) {
            sinsang.damage(amount)
        } else {
            spironus.logger.warning("Sinsang with UUID $uuid not found.")
        }
    }

    fun heal(uuid: String, amount: Double) {
        val sinsang = sinsangs[uuid]
        if (sinsang != null) {
            sinsang.heal(amount)
        } else {
            spironus.logger.warning("Sinsang with UUID $uuid not found.")
        }
    }

    fun destroy(uuid: String) {
        val sinsang = sinsangs[uuid]
        if (sinsang != null) {
            sinsang.destroy()
            sinsangs.remove(uuid)
            scope.yamlcfg.set(uuid, null)
            scope.save()
        } else {
            spironus.logger.warning("Sinsang with UUID $uuid not found.")
        }
    }

    private fun dist(loc1: Location, x: Double, z: Double): Double {
        return sqrt((loc1.x - x).pow(2.0) + (loc1.z - z).pow(2.0))
    }

    fun schedule() {
        spironus.server.scheduler.scheduleSyncRepeatingTask(spironus, {
            for(player in spironus.server.onlinePlayers) {
                var minDist = Double.MAX_VALUE
                var closestSinsang: Sinsang? = null
                for (sinsang in sinsangs.values) {
                    val distance = dist(player.location, sinsang.locX,sinsang.locZ)

                    if (distance < minDist) {
                        minDist = distance
                        closestSinsang = sinsang
                    }
                }

                if(minDist < 150 && closestSinsang != null) {
                    player.showBossBar(closestSinsang.bossbar)
                }

                for(sinsnags in sinsangs.values) {
                    if(sinsnags.uuid != closestSinsang?.uuid) {
                        player.hideBossBar(sinsnags.bossbar)
                    }
                }
            }
        }, 1, 1)
    }

    fun onDisable() {
        for(player in spironus.server.onlinePlayers) {
            for(sinsang in sinsangs.values) {
                player.hideBossBar(sinsang.bossbar)
            }
        }
    }
}