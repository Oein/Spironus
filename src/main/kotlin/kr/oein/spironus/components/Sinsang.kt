package kr.oein.spironus.components

import kr.oein.spironus.Spironus
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Slime
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

class Sinsang(val uuid: String, val spironus: Spironus) {
    val confDefaultHealth = 5000.0
    var slime: LivingEntity? = null
    var blockDisplay: BlockDisplay? = null
    var health: Double = confDefaultHealth
    var owner = "-1"
    var ownerTemaName = "미점령"

    var lastHitterName = ""

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

        section.set("blockDisplay", blockDisplay?.uniqueId?.toString())
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

        owner = section.getString("owner")?: "-1"
        health = section.getDouble("health", confDefaultHealth)
        locX = section.getDouble("locX", 0.0)
        locY = section.getDouble("locY", 0.0)
        locZ = section.getDouble("locZ", 0.0)
        name = section.getString("name") ?: "Unnamed"

        updateBossbarTitle()
    }

    var lastDamagedTeam = "-1"

    private fun checkDestroyed() {
        if (health <= 0) {
            health = confDefaultHealth
            owner = lastDamagedTeam
            save()
            spironus.logger.info("Sinsang $uuid has been destroyed.")
            onTaken(lastDamagedTeam)
        }
    }

    fun damage(amount: Double, team: String) {
        this.health -= amount
        this.lastDamagedTeam = team

        checkDestroyed()

        bossbar.progress((this.health / confDefaultHealth).toFloat())
        updateBossbarTitle()
    }

    fun destroy() {
        blockDisplay?.remove()
        if(slime is LivingEntity) {
            slime!!.remove()
        }
        spironus.sinsangManager.sinsangs.remove(uuid)
        spironus.kvdb.loadScope("sinsang").yamlcfg.set(uuid, null)
        spironus.kvdb.loadScope("sinsang").save()

        for(player in spironus.server.onlinePlayers)
            player.hideBossBar(bossbar)

        spironus.logger.info("Sinsang $uuid has been destroyed and removed from storage.")
    }

    fun onTaken(by: String) {
        val teamName = spironus.kvdb.loadScope("teams").yamlcfg.getConfigurationSection(by)?.getString("name") ?: "Unknown Team"
        spironus.server.broadcast(Component.text("신상 ${this.name}이(가) $teamName 팀에 의해 점령되었습니다."))
        for(player in spironus.server.onlinePlayers) {
            player.showTitle(
                Title.title(Component.text("신상 ${this.name}이(가) $teamName 팀에 의해 점령되었습니다.").color(NamedTextColor.GREEN),
                    Component.text("막타 : ${this.lastHitterName}").color(NamedTextColor.YELLOW))
            )
        }
        for(world in spironus.server.worlds) {
            world.playSound(
                org.bukkit.Location(world, locX, locY, locZ),
                org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE,
                10000.0f, 1.0f
            )
        }
    }

    fun updateBossbarTitle() {
        if(owner == "-1") {
            ownerTemaName = "미점령"
        } else {
            spironus.logger.info { "Owner: $owner" }
            val teamName = spironus.kvdb.loadScope("teams").yamlcfg.getConfigurationSection(owner)?.getString("name")
            ownerTemaName = teamName ?: "Unknown Team"
        }

        bossbar.name(
            Component.text("[$ownerTemaName] ", NamedTextColor.AQUA)
                .append { Component.text("$name ", NamedTextColor.GREEN) }
                .append { Component.text("(${this.health.toInt()} / $confDefaultHealth)", NamedTextColor.RED) }
        )
    }

    fun damage(amount: Double) {
        this.health -= amount
        checkDestroyed()
        bossbar.progress((this.health / confDefaultHealth).toFloat())
        updateBossbarTitle()
    }

    fun setLastAttackerName(name: String) {
        this.lastHitterName = name
    }

    fun heal(amount: Double) {
        this.health += amount
        if (this.health > confDefaultHealth) {
            this.health = confDefaultHealth
        }
        bossbar.progress((this.health / confDefaultHealth).toFloat())
        updateBossbarTitle()
    }

    val sinsangSize = 4
    fun spawnSlime() {
        if (slime == null || slime!!.isDead) {
            val world = spironus.server.worlds.firstOrNull() ?: return
            val loc = org.bukkit.Location(
                world,
                locX, locY, locZ
            )
            val slime = world.spawnEntity(loc, EntityType.SLIME)
            slime.isCustomNameVisible = false

            val lentity = slime as Slime
            lentity.setAI(false)
            lentity.isSilent = true
            lentity.isCustomNameVisible = false
            lentity.setGravity(false)
            lentity.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0))

            lentity.size = (sinsangSize * 2)
            lentity.registerAttribute(Attribute.MAX_HEALTH)
            lentity.getAttribute(Attribute.MAX_HEALTH)?.let { it.baseValue = 2048.0 }
            lentity.health = 2048.0

            val sinsangUUIDKey = NamespacedKey("spironus", "ss_uuid")
            val sinsangSessionKey = NamespacedKey("spironus", "ss_session")
            lentity.persistentDataContainer.set(
                sinsangUUIDKey,
                PersistentDataType.STRING,
                uuid
            )
            lentity.persistentDataContainer.set(
                sinsangSessionKey,
                PersistentDataType.STRING,
                spironus.sessionID
            )

            this.slime = lentity
        }
    }
}