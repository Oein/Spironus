package kr.oein.spironus.components

import kr.oein.spironus.Spironus

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
}