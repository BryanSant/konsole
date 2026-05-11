package tools.konsole.textual.binding

import tools.konsole.textual.events.Key

/**
 * A key→action binding. Mirrors Python textual's frozen-dataclass `Binding`.
 *
 * Bindings are typically declared in a widget's companion object:
 *
 * ```
 * companion object {
 *     val BINDINGS = bindings(
 *         "q" to "quit"           label "Quit",
 *         "ctrl+c" to "cancel"    label "Cancel",
 *     )
 * }
 * ```
 *
 * Or constructed directly:
 *
 * ```
 * val BINDINGS = listOf(Binding(key = "q", action = "quit", description = "Quit"))
 * ```
 *
 * @param key the key sequence in textual's syntax (`"q"`, `"ctrl+c"`, `"escape"`, etc).
 * @param action the action name to invoke when the key fires.
 * @param description user-visible label shown in the Footer widget.
 * @param show whether to display this binding in the Footer.
 * @param priority bindings with higher priority intercept the key before regular bindings.
 * @param system marks system bindings (Ctrl-C, Ctrl-Q) that can't be overridden lightly.
 */
public data class Binding(
    public val key: String,
    public val action: String,
    public val description: String = "",
    public val show: Boolean = true,
    public val priority: Boolean = false,
    public val system: Boolean = false,
)

/**
 * Map of key strings → [Binding]s. Mirrors textual's `BindingsMap`.
 *
 * Used by [tools.konsole.textual.widget.Widget] to dispatch keyboard input to
 * registered actions. Multiple bindings may share the same key (priority order).
 */
public class BindingsMap(initial: Collection<Binding> = emptyList()) {

    private val byKey: MutableMap<String, MutableList<Binding>> = mutableMapOf()

    init {
        for (b in initial) add(b)
    }

    public fun add(binding: Binding): BindingsMap = apply {
        byKey.getOrPut(binding.key) { mutableListOf() }.add(binding)
    }

    /** Look up a binding for [event]'s key. Returns the first match by priority, else regular order. */
    public fun match(event: Key): Binding? {
        for ((k, bindings) in byKey) {
            if (event.matches(k)) {
                return bindings.firstOrNull { it.priority } ?: bindings.firstOrNull()
            }
        }
        return null
    }

    /** All bindings, in registration order. */
    public fun all(): List<Binding> = byKey.values.flatten()

    public val isEmpty: Boolean get() = byKey.isEmpty()
    public val isNotEmpty: Boolean get() = !isEmpty
}

/** DSL: build a [BindingsMap] from a list of `"key" to "action"` pairs with optional descriptions. */
public fun bindings(vararg pairs: Pair<String, String>): BindingsMap {
    val map = BindingsMap()
    for ((k, a) in pairs) map.add(Binding(key = k, action = a))
    return map
}
