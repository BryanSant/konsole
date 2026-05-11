package tools.konsole.textual.dom

/**
 * Minimal CSS-selector parser + matcher for DOM queries. Mirrors a *subset*
 * of textual's `css/query.py` semantics: supports type (`Widget`), id (`#foo`),
 * class (`.bar`), and AND-compounds (`Widget.bar#foo`). Descendant combinators
 * (`Parent Child`) are honored.
 *
 * Phase 8 (the full TCSS engine) will replace this with a complete parser
 * including pseudo-classes (`:focus`, `:hover`, …), specificity calculation,
 * and the comma-separated selector lists.
 */
public class Selector(public val source: String) {

    private val segments: List<Compound> = parse(source)

    public fun matches(node: DOMNode): Boolean {
        if (segments.isEmpty()) return false
        val last = segments.last()
        if (!last.matches(node)) return false
        if (segments.size == 1) return true
        // Walk ancestors validating remaining segments right-to-left.
        var ancestor = node.parent
        var i = segments.size - 2
        while (i >= 0 && ancestor != null) {
            if (segments[i].matches(ancestor)) i -= 1
            ancestor = ancestor.parent
        }
        return i < 0
    }

    private data class Compound(val type: String?, val id: String?, val classes: Set<String>) {
        fun matches(node: DOMNode): Boolean {
            if (type != null && node.cssType != type) return false
            if (id != null && node.id != id) return false
            if (!classes.all { it in node.classes }) return false
            return true
        }
    }

    private fun parse(s: String): List<Compound> {
        return s.trim().split(Regex("\\s+")).map { parseCompound(it) }
    }

    private fun parseCompound(s: String): Compound {
        // Split at boundaries between '.', '#', or alpha runs
        var type: String? = null
        var id: String? = null
        val classes = mutableSetOf<String>()
        var i = 0
        // Optional leading type
        if (i < s.length && s[i].isLetter()) {
            val start = i
            while (i < s.length && s[i].let { it.isLetterOrDigit() || it == '_' }) i += 1
            type = s.substring(start, i)
        }
        while (i < s.length) {
            val tok = s[i]
            val start = i + 1
            i = start
            while (i < s.length && s[i].let { it.isLetterOrDigit() || it == '_' || it == '-' }) i += 1
            val name = s.substring(start, i)
            when (tok) {
                '#' -> id = name
                '.' -> classes += name
                else -> { /* unknown — skip */ }
            }
        }
        return Compound(type = type, id = id, classes = classes)
    }
}

/** Find the first node in the subtree rooted at [this] matching [selector]. Returns null if none. */
public fun DOMNode.queryOne(selector: String): DOMNode? {
    val sel = Selector(selector)
    return walk().firstOrNull { sel.matches(it) }
}

/** All nodes in the subtree rooted at [this] matching [selector]. */
public fun DOMNode.query(selector: String): List<DOMNode> {
    val sel = Selector(selector)
    return walk().filter { sel.matches(it) }.toList()
}
