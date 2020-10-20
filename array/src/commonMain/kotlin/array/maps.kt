package array

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentMapOf

class ImmutableMap2<K, V> private constructor(val content: PersistentMap<K, V>) : Map<K, V> by content {
    constructor() : this(persistentMapOf())

    fun copyAndPut(key: K, value: V): ImmutableMap2<K, V> {
        return ImmutableMap2(content.put(key, value))
    }

    fun copyAndPutMultiple(vararg valueList: Pair<K, V>): ImmutableMap2<K, V> {
        return ImmutableMap2(content.putAll(mapOf(*valueList)))
    }

    fun copyWithout(key: K): ImmutableMap2<K, V> {
        return if (containsKey(key)) {
            ImmutableMap2(content.remove(key))
        } else {
            this
        }
    }

    fun copyWithoutMultiple(keys: Array<K>): ImmutableMap2<K, V> {
        return if (keys.isNotEmpty()) {
            var curr = content
            keys.forEach { key ->
                curr = curr.remove(key)
            }
            ImmutableMap2(curr)
        } else {
            this
        }
    }

    companion object {
        fun <K, V> makeFromContent(content: List<Pair<K, V>>): ImmutableMap2<K, V> {
            val result = persistentHashMapOf(*content.toTypedArray())
            return ImmutableMap2(result)
        }
    }
}
