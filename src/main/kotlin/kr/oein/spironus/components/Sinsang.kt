package kr.oein.spironus.components

import kr.oein.spironus.Spironus
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Shulker
import java.util.UUID
import kotlin.math.floor

class Sinsang(val uuid: String, val spironus: Spironus) {
    var shulkers = mutableListOf<LivingEntity>()
    var blockDisplay: BlockDisplay? = null
    var health: Double = 10000.0
    var owner = "-1"
    var ownerTemaName = "신상"

    var locX: Double = 0.0
    var locY: Double = 0.0
    var locZ: Double = 0.0

    var name = "Unnamed"
    var bossbar = BossBar.bossBar(Component.text("[$ownerTemaName] $name"), 1.0f, BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_20)

    fun save() {
        val scope = spironus.kvdb.loadScope("sinsang")
        var section = scope.yamlcfg.getConfigurationSection(uuid)
        if (section == null) {
            scope.yamlcfg.createSection(uuid)
        }
        section = scope.yamlcfg.getConfigurationSection(uuid)!!

        val uuids = shulkers.map { it.uniqueId.toString() }
        section.set("blockDisplay", blockDisplay?.uniqueId?.toString())
        val shulkersSec = section.createSection("shulkers")
        shulkersSec.set("length", uuids.size)
        for (i in uuids.indices) {
            shulkersSec.set("$i", uuids[i])
        }
        section.set("owner", owner)
        section.set("health", health)
        section.set("locX", locX)
        section.set("locY", locY)
        section.set("locZ", locZ)
        section.set("name", name)

        scope.save()
    }

    fun load() {
        val scope = spironus.kvdb.loadScope("sinsang")
        val section = scope.yamlcfg.getConfigurationSection(uuid) ?: return

        val blockDisplayUuid = section.getString("blockDisplay")
        if (blockDisplayUuid != null) {
            val blockUUID = UUID.fromString(blockDisplayUuid)
            blockDisplay = spironus.server.getEntity(blockUUID) as? BlockDisplay
        }

        val shulkersSec = section.getConfigurationSection("shulkers")
        if (shulkersSec != null) {
            val length = shulkersSec.getInt("length", 0)
            for (i in 0 until length) {
                val shulkerUuid = shulkersSec.getString("$i")
                if (shulkerUuid != null) {
                    val shulkerEntity = spironus.server.getEntity(UUID.fromString(shulkerUuid))
                    if (shulkerEntity is Shulker) {
                        shulkers.add(shulkerEntity)
                    } else {
                        spironus.logger.warning("Sinsang: Shulker with UUID $shulkerUuid not found or is not a Shulker entity.")
                    }
                } else {
                    spironus.logger.warning("Sinsang: Shulker UUID at index $i is null.")
                }
            }
        }

        owner = section.getString("owner")?: "-1"
        health = section.getDouble("health", 10000.0)
        locX = section.getDouble("locX", 0.0)
        locY = section.getDouble("locY", 0.0)
        locZ = section.getDouble("locZ", 0.0)
        name = section.getString("name") ?: "Unnamed"

        updateBossbarTitle()
    }

    var lastDamagedTeam = "-1"

    fun damage(amount: Double, team: String) {
        this.health -= amount
        this.lastDamagedTeam = team

        if(this.health <= 0) {
            this.health = 10000.0
            this.owner = this.lastDamagedTeam
            save()
            spironus.logger.info("Sinsang $uuid has been destroyed.")
        }

        bossbar.progress((this.health / 10000.0).toFloat())
        updateBossbarTitle()
    }

    fun destroy() {
        blockDisplay?.remove()
        for (shulker in shulkers) {
            shulker.remove()
        }
        spironus.sinsangManager.sinsangs.remove(uuid)
        spironus.kvdb.loadScope("sinsang").yamlcfg.set(uuid, null)
        spironus.kvdb.loadScope("sinsang").save()
        spironus.logger.info("Sinsang $uuid has been destroyed and removed from storage.")
    }

    fun updateBossbarTitle() {
        if(owner == "-1") {
            ownerTemaName = "신상"
        } else {
            spironus.logger.info { "Owner: $owner" }
            val teamName = spironus.kvdb.loadScope("teams").yamlcfg.getConfigurationSection(owner)?.getString("name")
            ownerTemaName = teamName ?: "Unknown Team"
        }
        bossbar.name(Component.text("[$ownerTemaName] $name (${this.health.toInt()} / 10000)"))
    }

    fun damage(amount: Double) {
        this.health -= amount
        if (this.health <= 0) {
            this.health = 10000.0
            this.owner = this.lastDamagedTeam
            save()
            spironus.logger.info("Sinsang $uuid has been destroyed.")
        }
        bossbar.progress((this.health / 10000.0).toFloat())
        updateBossbarTitle()
    }

    fun heal(amount: Double) {
        this.health += amount
        if (this.health > 10000.0) {
            this.health = 10000.0
        }
        bossbar.progress((this.health / 10000.0).toFloat())
        updateBossbarTitle()
    }
}