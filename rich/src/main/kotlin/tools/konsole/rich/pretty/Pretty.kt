package tools.konsole.rich.pretty

import tools.konsole.rich.Console
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Segment
import tools.konsole.rich.Style
import tools.konsole.rich.Theme
import tools.konsole.rich.text.Justify
import tools.konsole.core.style.Color

/**
 * Pretty-print arbitrary Kotlin objects with type-aware coloring. Mirrors `rich.pretty.Pretty`.
 *
 * Supports collections (List/Set/Map/Array), primitives, strings, and falls back to `toString()`
 * for everything else. Uses the console theme's `repr.*` style names so output matches rich.
 */
public class Pretty(
    public val target: Any?,
    public val indentSize: Int = 4,
    public val maxLength: Int? = null,
    public val maxString: Int? = null,
    public val maxDepth: Int? = null,
    public val expandAll: Boolean = false,
    public val indentGuides: Boolean = false,
    public val justify: Justify = Justify.Default,
) : Measurable {

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> = sequence {
        val theme = console.theme
        val sb = SegmentBuilder(theme)
        render(sb, target, depth = 0, options.maxWidth)
        for (s in sb.segments) yield(s)
    }

    override fun measure(console: Console, options: RenderOptions): Measurement =
        Measurement(0, options.maxWidth)

    private fun render(sb: SegmentBuilder, value: Any?, depth: Int, maxWidth: Int) {
        if (maxDepth != null && depth >= maxDepth) {
            sb.dim("...")
            return
        }
        when (value) {
            null -> sb.style("repr.none", "null")
            is Boolean -> sb.style(if (value) "repr.bool_true" else "repr.bool_false", value.toString())
            is Number -> sb.style("repr.number", value.toString())
            is Char -> { sb.style("repr.str", "'$value'") }
            is String -> renderString(sb, value)
            is List<*> -> renderList(sb, value, "[", "]", depth, maxWidth)
            is Set<*> -> renderList(sb, value.toList(), "{", "}", depth, maxWidth)
            is Map<*, *> -> renderMap(sb, value, depth, maxWidth)
            is Array<*> -> renderList(sb, value.toList(), "[", "]", depth, maxWidth)
            is BooleanArray -> renderList(sb, value.toList(), "[", "]", depth, maxWidth)
            is IntArray -> renderList(sb, value.toList(), "[", "]", depth, maxWidth)
            is LongArray -> renderList(sb, value.toList(), "[", "]", depth, maxWidth)
            is DoubleArray -> renderList(sb, value.toList(), "[", "]", depth, maxWidth)
            is FloatArray -> renderList(sb, value.toList(), "[", "]", depth, maxWidth)
            else -> renderObject(sb, value, depth, maxWidth)
        }
    }

    private fun renderString(sb: SegmentBuilder, value: String) {
        val truncated = if (maxString != null && value.length > maxString) value.take(maxString) + "…" else value
        // Use Python-ish repr: prefer double-quoted unless the string contains double quotes only.
        val q: Char = if (truncated.contains('"') && !truncated.contains('\'')) '\'' else '"'
        sb.style("repr.str", buildString {
            append(q)
            for (c in truncated) {
                when (c) {
                    q -> { append('\\'); append(q) }
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(c)
                }
            }
            append(q)
        })
    }

    private fun renderList(sb: SegmentBuilder, items: List<*>, open: String, close: String, depth: Int, maxWidth: Int) {
        sb.style("repr.brace", open)
        val list = if (maxLength != null && items.size > maxLength) items.take(maxLength) else items
        for ((i, item) in list.withIndex()) {
            if (i > 0) { sb.style("repr.comma", ", ") }
            render(sb, item, depth + 1, maxWidth)
        }
        if (maxLength != null && items.size > maxLength) {
            sb.style("repr.comma", ", "); sb.dim("… +${items.size - maxLength} more")
        }
        sb.style("repr.brace", close)
    }

    private fun renderMap(sb: SegmentBuilder, map: Map<*, *>, depth: Int, maxWidth: Int) {
        sb.style("repr.brace", "{")
        val entries = map.entries.toList()
        val list = if (maxLength != null && entries.size > maxLength) entries.take(maxLength) else entries
        for ((i, entry) in list.withIndex()) {
            if (i > 0) sb.style("repr.comma", ", ")
            render(sb, entry.key, depth + 1, maxWidth)
            sb.style("repr.attrib_equal", ": ")
            render(sb, entry.value, depth + 1, maxWidth)
        }
        if (maxLength != null && entries.size > maxLength) {
            sb.style("repr.comma", ", "); sb.dim("… +${entries.size - maxLength} more")
        }
        sb.style("repr.brace", "}")
    }

    private fun renderObject(sb: SegmentBuilder, value: Any, depth: Int, maxWidth: Int) {
        val klass = value::class
        sb.style("repr.tag_name", klass.simpleName ?: "Object")
        sb.style("repr.brace", "(")
        // Use reflection to discover member properties via Java fields.
        val fields = collectFields(value::class.java)
        var first = true
        for (f in fields) {
            if (!first) sb.style("repr.comma", ", ")
            sb.style("repr.attrib_name", f.name)
            sb.style("repr.attrib_equal", "=")
            val v = try { f.get(value) } catch (_: Exception) { "<inaccessible>" }
            render(sb, v, depth + 1, maxWidth)
            first = false
        }
        sb.style("repr.brace", ")")
    }

    private fun collectFields(klass: Class<*>): List<java.lang.reflect.Field> {
        val out = mutableListOf<java.lang.reflect.Field>()
        var cur: Class<*>? = klass
        val seen = mutableSetOf<String>()
        while (cur != null && cur != Any::class.java) {
            for (f in cur.declaredFields) {
                if (f.isSynthetic) continue
                if (java.lang.reflect.Modifier.isStatic(f.modifiers)) continue
                if (f.name.startsWith("$")) continue
                if (seen.add(f.name)) {
                    f.trySetAccessible()
                    out += f
                }
            }
            cur = cur.superclass
        }
        return out
    }

    /** A small builder around [Segment]s with theme-style lookup. */
    private class SegmentBuilder(val theme: Theme) {
        val segments: MutableList<Segment> = mutableListOf()
        fun style(name: String, text: String) {
            val s = theme[name]
            segments += Segment(text, if (s == null || s.isNull) null else s)
        }
        fun raw(text: String) { segments += Segment(text) }
        fun dim(text: String) { segments += Segment(text, Style(dim = true)) }
    }
}

/** Convenience: print a Pretty-formatted value. */
public fun Console.pprint(
    value: Any?,
    indentSize: Int = 4,
    maxLength: Int? = null,
    maxString: Int? = null,
    maxDepth: Int? = null,
) {
    print(Pretty(value, indentSize, maxLength, maxString, maxDepth))
}
