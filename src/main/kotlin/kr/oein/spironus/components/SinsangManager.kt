package kr.oein.spironus.components

import kr.oein.spironus.Spironus
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Slime
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

    fun setLastHitterName(uuid: String, person: String) {
        val sinsang = sinsangs[uuid]
        if (sinsang != null) {
            sinsang.setLastHitterName___(person)
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

    fun glowPlayer(player: org.bukkit.entity.Player) {
        player.addPotionEffect(
            org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.GLOWING,
                2, // 5 seconds
                1, // Level 1
                false, // Ambient
                false, // Particles
                false // Show in action bar
            )
        )
    }

    fun schedule() {
        spironus.server.scheduler.scheduleSyncRepeatingTask(spironus, {
            for(player in spironus.server.onlinePlayers) {
                var shownSinsangs: List<String> = mutableListOf()
                for (sinsang in sinsangs.values) {
                    val distance = dist(player.location, sinsang.locX,sinsang.locZ)

                    if (distance < 150) {
                        player.showBossBar(sinsang.bossbar)
                        shownSinsangs = shownSinsangs + sinsang.uuid
                    }
                    val crc = spironus.kvdb.get("playerinfo-uuid2crc", player.uniqueId.toString())

                    if(distance < 20) {
                        if(crc is String) {
                            val team = spironus.kvdb.get("playerinfo-team", crc)
                            if(team is String && team != "-1") {
                                if(sinsang.owner != team) glowPlayer(player)
                            } else glowPlayer(player)
                        } else glowPlayer(player)
                    }
                }

                for(sinsnags in sinsangs.values) {
                    if(sinsnags.uuid !in shownSinsangs) {
                        player.hideBossBar(sinsnags.bossbar)
                    }
                }
            }


        }, 1, 1)

        spironus.server.scheduler.scheduleSyncRepeatingTask(spironus, {
            for(entity in spironus.server.worlds.flatMap { it.entities }) {
                if(entity !is Slime) continue
                val sinsangUUIDKey = NamespacedKey("spironus", "ss_uuid")
                val sinsangSessionKey = NamespacedKey("spironus", "ss_session")
                if(entity.persistentDataContainer.has(sinsangUUIDKey)) {
                    val sinsangUUID = entity.persistentDataContainer.get(sinsangUUIDKey, org.bukkit.persistence.PersistentDataType.STRING)
                    if (sinsangUUID != null && sinsangs.containsKey(sinsangUUID)) {
                        val sinsang = sinsangs[sinsangUUID]
                        if(sinsang == null) {
                            entity.remove()
                        }
                        if(entity.persistentDataContainer.has(sinsangSessionKey)) {
                            val session = entity.persistentDataContainer.get(sinsangSessionKey, org.bukkit.persistence.PersistentDataType.STRING)
                            if(session != spironus.sessionID) {
                                entity.remove()
                            }
                        } else {
                            entity.remove()
                        }
                        sinsang?.spawnSlime()
                    } else {
                        entity.remove()
                    }
                }
            }

            for(sinsang in sinsangs.values) {
                sinsang.spawnSlime()
            }
        }, 20, 20)
    }

    fun onDisable() {
        for(player in spironus.server.onlinePlayers) {
            for(sinsang in sinsangs.values) {
                player.hideBossBar(sinsang.bossbar)
            }
        }
    }
}