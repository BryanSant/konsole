package tools.konsole.rich

/**
 * Bounded LRU (least-recently-used) cache. Backed by a [LinkedHashMap] with
 * access-order eviction. Not threadsafe — use [java.util.Collections.synchronizedMap]
 * if shared.
 *
 * Used by hot paths (style → SGR cache, stylesheet rule-match cache) where re-computation
 * is expensive but the working set is small.
 */
public class LRUCache<K, V>(public val capacity: Int) {

    init {
        require(capacity > 0) { "capacity must be > 0, was $capacity" }
    }

    private val map = object : LinkedHashMap<K, V>(capacity, 0.75f, /* accessOrder = */ true) {
        override fun removeEldestEntry(eldest: Map.Entry<K, V>?): Boolean = size > capacity
    }

    public val size: Int get() = map.size

    public operator fun get(key: K): V? = map[key]

    public operator fun set(key: K, value: V) {
        map[key] = value
    }

    public fun getOrPut(key: K, default: () -> V): V {
        map[key]?.let { return it }
        val v = default()
        map[key] = v
        return v
    }

    public fun clear(): Unit = map.clear()

    public operator fun contains(key: K): Boolean = map.containsKey(key)
}

/**
 * Bounded FIFO (first-in-first-out) cache. Insertion-order eviction.
 * Cheaper than [LRUCache] when access patterns are uniform.
 */
public class FIFOCache<K, V>(public val capacity: Int) {

    init {
        require(capacity > 0) { "capacity must be > 0, was $capacity" }
    }

    private val map = object : LinkedHashMap<K, V>(capacity, 0.75f, /* accessOrder = */ false) {
        override fun removeEldestEntry(eldest: Map.Entry<K, V>?): Boolean = size > capacity
    }

    public val size: Int get() = map.size

    public operator fun get(key: K): V? = map[key]

    public operator fun set(key: K, value: V) {
        map[key] = value
    }

    public fun getOrPut(key: K, default: () -> V): V {
        map[key]?.let { return it }
        val v = default()
        map[key] = v
        return v
    }

    public fun clear(): Unit = map.clear()

    public operator fun contains(key: K): Boolean = map.containsKey(key)
}
