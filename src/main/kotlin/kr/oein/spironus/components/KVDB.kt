package kr.oein.spironus.components

import kr.oein.spironus.Spironus

class KVDB(val spironus: Spironus) {
    var scopes: MutableMap<String, KVDBScope> = mutableMapOf()

    fun loadScope(scope: String): KVDBScope {
        if (scopes.containsKey(scope)) return scopes[scope]!!
        return KVDBScope(spironus, scope).also {
            it.load()
            scopes[scope] = it
        }
    }

    fun get(scope: String, key: String): Any? {
        return loadScope(scope).get(key)
    }

    fun set(scope: String, key: String, value: Any) {
        loadScope(scope).set(key, value)
    }

    fun remove(scope: String, key: String) {
        loadScope(scope).remove(key)
    }

    fun has(scope: String, key: String): Boolean {
        return loadScope(scope).has(key)
    }

    fun keys(scope: String): Set<String> {
        return loadScope(scope).keys()
    }

    fun clear(scope: String) {
        loadScope(scope).clear()
    }
}