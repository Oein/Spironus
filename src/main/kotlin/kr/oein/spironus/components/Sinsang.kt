package kr.oein.spironus.components

import kr.oein.spironus.Spironus
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Shulker
import java.util.UUID

class Sinsang(val uuid: String, val spironus: Spironus) {
    var shulkers = mutableListOf<LivingEntity>()
    var blockDisplay: BlockDisplay? = null

    public fun save() {
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

        scope.save()
    }

    public fun load() {
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
    }
}