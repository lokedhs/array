package array

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

// This is a test implementation of an immutable map. This is not meant to be a proper implementation
// but as a reference for testing once the real implementation is completed.

class ImmutableMap3<K, V> private constructor(content: HashMap<K, V>) : Map<K, V> by content {
    constructor() : this(HashMap<K, V>())

    fun copyAndPut(key: K, value: V): ImmutableMap3<K, V> {
        val new = copyMap()
        new[key] = value
        return ImmutableMap3(new)
    }

    fun copyAndPutMultiple(vararg content: Pair<K, V>): ImmutableMap3<K, V> {
        val new = copyMap()
        fillContent(new, content)
        return ImmutableMap3(new)
    }

    fun copyWithout(key: K): ImmutableMap3<K, V> {
        return if (containsKey(key)) {
            ImmutableMap3(copyMap(key))
        } else {
            this
        }
    }

    private fun copyMap(ignoreKey: K? = null): HashMap<K, V> {
        val new = HashMap<K, V>()
        this.forEach { (key, value) ->
            if (ignoreKey == null || key != ignoreKey) {
                new[key] = value
            }
        }
        return new
    }

    companion object {
        fun <K, V> makeFromContent(content: List<Pair<K, V>>): ImmutableMap3<K, V> {
            val map = HashMap<K, V>()
            content.forEach { (key, value) ->
                map[key] = value
            }
            return ImmutableMap3(map)
        }

        private fun <K, V> fillContent(map: HashMap<K, V>, content: Array<out Pair<K, V>>) {
            content.forEach { (key, value) ->
                map[key] = value
            }
        }
    }
}

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
}
