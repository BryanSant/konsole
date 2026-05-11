package tools.konsole.rich

/**
 * Mirrors rich's `highlight.py`: a [Highlighter] inspects [Text] and stylizes
 * spans it recognises (numbers, paths, UUIDs, URLs, brackets, etc.).
 *
 * Concrete subclasses typically extend [RegexHighlighter] and supply a list
 * of patterns whose named groups become style-class names looked up via the
 * console's [Theme]. The base [Highlighter] just defines the protocol.
 */
public fun interface Highlighter {
    /** Apply highlighting to [text] in-place. */
    public fun highlight(text: Text)

    public companion object {
        /** No-op highlighter. */
        public val NULL: Highlighter = Highlighter { /* nothing */ }
    }
}

/**
 * Regex-based highlighter. Each pattern's *named groups* are stylized via
 * [Theme] entries. Group name `"foo"` maps to theme key `"<base>.foo"` if a
 * [base] prefix is set, else just `"foo"`.
 *
 * Matching is non-overlapping in the order patterns are listed; later patterns
 * may stylize substrings already touched by earlier patterns (last-write-wins).
 */
public open class RegexHighlighter(
    public val patterns: List<Regex>,
    public val theme: Theme = Theme.DEFAULT,
    public val base: String? = null,
) : Highlighter {

    override fun highlight(text: Text) {
        val plain = text.plain
        for (pattern in patterns) {
            for (match in pattern.findAll(plain)) {
                for ((name, _) in groupNames(pattern)) {
                    val group = match.groups[name] ?: continue
                    val key = if (base.isNullOrEmpty()) name else "$base.$name"
                    val style = theme[key] ?: continue
                    text.stylize(group.range.first, group.range.last + 1, style)
                }
            }
        }
    }

    /** Return the named groups in [pattern]. Best-effort via reflective parse of the source. */
    private fun groupNames(pattern: Regex): List<Pair<String, IntRange?>> {
        val src = pattern.pattern
        val out = mutableListOf<Pair<String, IntRange?>>()
        val rx = Regex("""\(\?P?<([A-Za-z_][A-Za-z0-9_]*)>""")
        for (m in rx.findAll(src)) {
            out += m.groupValues[1] to m.range
        }
        return out
    }
}

/**
 * Default highlighter — recognises numbers, hex, UUIDs, paths, URLs, IPv4/IPv6,
 * call-style identifiers, and common bracket pairs. Mirrors rich's `ReprHighlighter`.
 */
public class DefaultHighlighter(theme: Theme = Theme.DEFAULT) : RegexHighlighter(
    patterns = DEFAULT_PATTERNS,
    theme = theme,
    base = "repr",
) {
    public companion object {
        public val DEFAULT_PATTERNS: List<Regex> = listOf(
            // brackets and parens
            Regex("""(?<brace>[{}\[\]()])"""),
            // hex
            Regex("""(?<number>0x[0-9a-fA-F]+)"""),
            // floats and ints
            Regex("""(?<number>(?<!\w)-?\d+\.?\d*)"""),
            // UUIDs
            Regex("""(?<uuid>[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})"""),
            // URLs
            Regex("""(?<url>https?://[^\s)>]+)"""),
            // file paths (POSIX)
            Regex("""(?<path>(?<!\w)/(?:[^/\s]+/)*[^/\s]+)"""),
            // IPv4
            Regex("""(?<ipv4>(?<!\d)(?:\d{1,3}\.){3}\d{1,3}(?!\d))"""),
            // call-style identifiers: foo.bar(
            Regex("""(?<call>[A-Za-z_][\w.]*)(?=\()"""),
            // single/double-quoted strings
            Regex("""(?<str>"[^"\\]*(?:\\.[^"\\]*)*"|'[^'\\]*(?:\\.[^'\\]*)*')"""),
            // booleans / None / null  (Java named groups can't contain underscores)
            Regex("""(?<!\w)(?<boolTrue>True|true)(?!\w)"""),
            Regex("""(?<!\w)(?<boolFalse>False|false)(?!\w)"""),
            Regex("""(?<!\w)(?<none>None|null|nil)(?!\w)"""),
        )
    }
}
