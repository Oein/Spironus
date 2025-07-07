package kr.oein.spironus.components

import kr.oein.spironus.Spironus
import org.bukkit.configuration.file.YamlConfiguration

class KVDB_Scope(val spironus: Spironus, val scope: String) {
    var yamlcfg: YamlConfiguration = YamlConfiguration()

    fun load() {
        // load from db.yml
        val file = spironus.dataFolder.resolve("$scope.yml")
        if (file.exists()) {
            // Load the data from the file
            yamlcfg = YamlConfiguration.loadConfiguration(file)
        }
    }

    fun save() {
        // Save the data to db.yml
        val file = spironus.dataFolder.resolve("$scope.yml")
        yamlcfg.save(file)
    }

    fun get(key: String): Any? {
        return yamlcfg.get(key)
    }

    fun set(key: String, value: Any) {
        yamlcfg.set(key, value)
        save() // Save after setting a new value
    }

    fun remove(key: String) {
        yamlcfg.set(key, null)
        save() // Save after removing a value
    }

    fun has(key: String): Boolean {
        return yamlcfg.contains(key)
    }

    fun keys(): Set<String> {
        return yamlcfg.getKeys(false)
    }

    fun clear() {
        yamlcfg = YamlConfiguration()
        save() // Save after clearing the configuration
    }
}