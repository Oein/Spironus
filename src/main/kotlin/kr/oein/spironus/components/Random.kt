package kr.oein.spironus.components

class Random {
    fun generate(): String {
        val charset = ('a'..'z') + ('0'..'9')
        return (1..6)
            .map { charset.random() }
            .joinToString("")
    }
}