package tools.konsole.rich.json

import tools.konsole.rich.Console
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.syntax.Syntax
import tools.konsole.rich.syntax.SyntaxTheme

/**
 * Pretty JSON renderable. Mirrors `rich.json.JSON`.
 *
 * Accepts either a raw JSON string or arbitrary Kotlin data via [fromData] (Map/List/Number/Boolean/String/null).
 */
public class Json(
    public val json: String,
    public val indent: Int = 2,
    public val theme: SyntaxTheme = SyntaxTheme.MONOKAI,
    public val highlight: Boolean = true,
) : Measurable {

    private val pretty: String by lazy { reformat(json, indent) }

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> {
        if (!highlight) {
            return sequenceOf(Segment(pretty))
        }
        return Syntax(pretty, "json", theme = theme).render(console, options)
    }

    override fun measure(console: Console, options: RenderOptions): Measurement {
        val widest = pretty.lines().maxOfOrNull { it.length } ?: 0
        return Measurement(0.coerceAtMost(widest), widest.coerceAtMost(options.maxWidth))
    }

    public companion object {
        /**
         * Build a pretty-printed JSON document from arbitrary Kotlin data.
         * Mirrors rich's `JSON.from_data`.
         *
         * @param data Map/List/Array/primitive/null/String to render.
         * @param indent indent step in spaces. Defaults to 2 (matches rich).
         * @param theme syntax theme for the rendered document.
         * @param highlight enable syntax highlighting. Defaults to true.
         * @param skipKeys when true, map keys that aren't [String]/[Number]/[Boolean]/`null`
         *   are silently skipped. Mirrors rich's `skip_keys`.
         * @param ensureAscii when true, all non-ASCII code points are emitted as `\uXXXX`
         *   escapes. Mirrors rich's `ensure_ascii`.
         */
        public fun fromData(
            data: Any?,
            indent: Int = 2,
            theme: SyntaxTheme = SyntaxTheme.MONOKAI,
            highlight: Boolean = true,
            skipKeys: Boolean = false,
            ensureAscii: Boolean = false,
        ): Json {
            val sb = StringBuilder()
            writeValue(sb, data, indent, 0, ensureAscii, skipKeys)
            return Json(sb.toString(), indent = indent, theme = theme, highlight = highlight)
        }

        private fun writeValue(sb: StringBuilder, value: Any?, indent: Int, depth: Int, ensureAscii: Boolean, skipKeys: Boolean) {
            when (value) {
                null -> sb.append("null")
                is Boolean -> sb.append(value.toString())
                is Number -> sb.append(value.toString())
                is String -> sb.append(quote(value, ensureAscii))
                is Map<*, *> -> writeObject(sb, value, indent, depth, ensureAscii, skipKeys)
                is Iterable<*> -> writeArray(sb, value, indent, depth, ensureAscii, skipKeys)
                is Array<*> -> writeArray(sb, value.asIterable(), indent, depth, ensureAscii, skipKeys)
                else -> sb.append(quote(value.toString(), ensureAscii))
            }
        }

        private fun writeArray(sb: StringBuilder, list: Iterable<*>, indent: Int, depth: Int, ensureAscii: Boolean, skipKeys: Boolean) {
            val items = list.toList()
            if (items.isEmpty()) { sb.append("[]"); return }
            sb.append("[\n")
            val pad = " ".repeat(indent * (depth + 1))
            for ((i, item) in items.withIndex()) {
                sb.append(pad)
                writeValue(sb, item, indent, depth + 1, ensureAscii, skipKeys)
                if (i < items.lastIndex) sb.append(",")
                sb.append("\n")
            }
            sb.append(" ".repeat(indent * depth))
            sb.append("]")
        }

        private fun writeObject(sb: StringBuilder, map: Map<*, *>, indent: Int, depth: Int, ensureAscii: Boolean, skipKeys: Boolean) {
            val entries = map.entries.filter { (k, _) ->
                !skipKeys || k is String || k is Number || k is Boolean || k == null
            }
            if (entries.isEmpty()) { sb.append("{}"); return }
            sb.append("{\n")
            val pad = " ".repeat(indent * (depth + 1))
            for ((i, entry) in entries.withIndex()) {
                sb.append(pad)
                sb.append(quote(entry.key.toString(), ensureAscii))
                sb.append(": ")
                writeValue(sb, entry.value, indent, depth + 1, ensureAscii, skipKeys)
                if (i < entries.lastIndex) sb.append(",")
                sb.append("\n")
            }
            sb.append(" ".repeat(indent * depth))
            sb.append("}")
        }

        private fun quote(s: String, ensureAscii: Boolean = false): String = buildString {
            append('"')
            for (ch in s) {
                when (ch) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    else -> when {
                        ch.code < 0x20 -> append("\\u%04x".format(ch.code))
                        ensureAscii && ch.code > 0x7E -> append("\\u%04x".format(ch.code))
                        else -> append(ch)
                    }
                }
            }
            append('"')
        }

        /**
         * Re-indent an already-valid JSON string. Lightweight: only restructures whitespace, not
         * value formatting (so `1.0e+5` stays as-is, etc).
         */
        private fun reformat(json: String, indent: Int): String {
            val sb = StringBuilder()
            var depth = 0
            var inString = false
            var escape = false
            val pad: () -> Unit = { sb.append(" ".repeat(indent * depth)) }
            for (ch in json) {
                if (inString) {
                    sb.append(ch)
                    if (escape) escape = false
                    else if (ch == '\\') escape = true
                    else if (ch == '"') inString = false
                    continue
                }
                when (ch) {
                    '"' -> { sb.append(ch); inString = true }
                    '{', '[' -> { sb.append(ch); depth += 1; sb.append('\n'); pad() }
                    '}', ']' -> { sb.append('\n'); depth = (depth - 1).coerceAtLeast(0); pad(); sb.append(ch) }
                    ',' -> { sb.append(ch); sb.append('\n'); pad() }
                    ':' -> sb.append(": ")
                    ' ', '\n', '\r', '\t' -> { /* skip whitespace */ }
                    else -> sb.append(ch)
                }
            }
            return sb.toString()
        }
    }
}
