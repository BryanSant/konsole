package tools.konsole.rich

/**
 * A named-style lookup table with optional parent inheritance.
 *
 * Markup tags like `[error]oops[/]` look up "error" via [get], walking the parent chain
 * until a match is found.
 */
public class Theme(
    public val styles: Map<String, Style>,
    public val parent: Theme? = null,
) {
    public operator fun get(name: String): Style? =
        styles[name] ?: parent?.get(name)

    public operator fun contains(name: String): Boolean =
        styles.containsKey(name) || parent?.contains(name) == true

    /** Return a new Theme with the given overrides layered on top of this one. */
    public operator fun plus(overrides: Map<String, Style>): Theme =
        Theme(overrides, parent = this)

    /** Convenience: layer a single (name, style) override. */
    public fun with(name: String, style: Style): Theme =
        Theme(mapOf(name to style), parent = this)

    public companion object {
        /** The built-in default theme (error/warn/info/etc). */
        public val DEFAULT: Theme = Theme(BuiltinThemeStyles)
    }
}
