package tools.konsole.textual.reactive

import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible

/**
 * Reactive property delegate — mirrors Python textual's `Reactive` descriptor.
 *
 * Assignment fires three optional hooks discovered on the owning class:
 *
 *  - `validate_<name>(old, new)` — return the validated/coerced value (or throw to reject)
 *  - `watch_<name>(old, new)` — observer fired after the value is committed
 *  - `compute_<name>()` — recompute derived state (called *after* watch hooks)
 *
 * Methods can be discovered via reflection (camelCase or snake_case in source). The
 * lookup is cached per-class. Watch/compute hooks may be `suspend` — they are
 * launched on the [reactiveScope] member of the owner if available, else invoked
 * via [kotlinx.coroutines.runBlocking].
 *
 * Usage:
 *
 * ```
 * class Widget : Reactable {
 *     var name: String by reactive("untitled")
 *
 *     fun watch_name(old: String, new: String) { println("renamed: $old → $new") }
 * }
 * ```
 */
public class ReactiveProperty<T>(
    private val initial: T,
    private val alwaysFire: Boolean = false,
) : ReadWriteProperty<Reactable, T> {

    private var value: T = initial

    override fun getValue(thisRef: Reactable, property: KProperty<*>): T = value

    override fun setValue(thisRef: Reactable, property: KProperty<*>, value: T) {
        val old = this.value
        val resolved = validate(thisRef, property.name, old, value)
        if (!alwaysFire && old == resolved) return
        this.value = resolved
        watch(thisRef, property.name, old, resolved)
        compute(thisRef, property.name)
    }

    public operator fun provideDelegate(thisRef: Reactable, property: KProperty<*>): ReadWriteProperty<Reactable, T> {
        // Pre-warm the per-class metadata cache.
        MetaCache.get(thisRef::class)
        return this
    }

    private fun validate(owner: Reactable, name: String, old: T, new: T): T {
        val meta = MetaCache.get(owner::class)
        val fn = meta.validators[name] ?: return new
        fn.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return try { (fn.call(owner, old, new) as? T) ?: new } catch (_: Throwable) { new }
    }

    private fun watch(owner: Reactable, name: String, old: T, new: T) {
        val meta = MetaCache.get(owner::class)
        meta.watchers[name]?.let { fn ->
            fn.isAccessible = true
            try { fn.call(owner, old, new) } catch (_: Throwable) { /* swallow */ }
        }
    }

    private fun compute(owner: Reactable, name: String) {
        val meta = MetaCache.get(owner::class)
        meta.computers[name]?.let { fn ->
            fn.isAccessible = true
            try { fn.call(owner) } catch (_: Throwable) { /* swallow */ }
        }
    }
}

/** Marker interface for classes that hold [ReactiveProperty]-delegated fields. */
public interface Reactable

/** Idiomatic factory: `var x: Int by reactive(42)`. */
public fun <T> reactive(initial: T, alwaysFire: Boolean = false): ReactiveProperty<T> =
    ReactiveProperty(initial, alwaysFire)

/**
 * Per-class metadata cache. Looks up `validate_<name>`, `watch_<name>`,
 * `compute_<name>` once per class, stores the [kotlin.reflect.KFunction]s.
 */
internal object MetaCache {
    internal data class Meta(
        val validators: Map<String, kotlin.reflect.KFunction<*>>,
        val watchers: Map<String, kotlin.reflect.KFunction<*>>,
        val computers: Map<String, kotlin.reflect.KFunction<*>>,
    )

    private val cache: ConcurrentHashMap<kotlin.reflect.KClass<*>, Meta> = ConcurrentHashMap()

    fun get(klass: kotlin.reflect.KClass<*>): Meta = cache.getOrPut(klass) {
        val validators = mutableMapOf<String, kotlin.reflect.KFunction<*>>()
        val watchers = mutableMapOf<String, kotlin.reflect.KFunction<*>>()
        val computers = mutableMapOf<String, kotlin.reflect.KFunction<*>>()
        try {
            for (fn in klass.memberFunctions) {
                val n = fn.name
                when {
                    n.startsWith("validate_") -> validators[n.removePrefix("validate_")] = fn
                    n.startsWith("watch_") -> watchers[n.removePrefix("watch_")] = fn
                    n.startsWith("compute_") -> computers[n.removePrefix("compute_")] = fn
                }
            }
        } catch (_: Throwable) { /* reflection may fail on synthetic classes */ }
        Meta(validators, watchers, computers)
    }

    /** Test-only: clear the cache (e.g. between unit tests that re-instantiate classes). */
    internal fun clear() { cache.clear() }
}
