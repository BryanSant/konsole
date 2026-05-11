package tools.konsole.rich.markup

import tools.konsole.rich.Style
import tools.konsole.rich.StyleParser
import tools.konsole.rich.Text
import tools.konsole.rich.Theme
import tools.konsole.rich.emoji.EmojiMap

/**
 * One-pass parser for rich-style markup.
 *
 * Grammar (informal):
 *   - `\\[`    → literal `[`
 *   - `[tag]`  → push style derived from `tag`
 *   - `[/]`    → pop one style
 *   - `[/tag]` → pop matching tag (alias for [/])
 *   - `:emoji_name:` → emoji substitution
 *   - everything else → literal text
 *
 * Tag content (`tag`) may be:
 *   - a theme name (looked up via [Theme])
 *   - a compound style string (`bold red on white`, parsed via [StyleParser])
 *   - `link=<URL>` → adds [Style.link]
 *   - mixed (tokens combined left-to-right)
 *
 * Unmatched closes at empty stack are ignored. Unmatched opens at EOF are auto-closed.
 */
public object Markup {

    private val EMOJI_PATTERN = Regex(":([a-zA-Z0-9_+-]+):")

    public fun parse(input: String, theme: Theme = Theme.DEFAULT): Text {
        val text = Text()
        val stack: ArrayDeque<Pair<String, Style>> = ArrayDeque()
        var i = 0
        val n = input.length
        val cur = StringBuilder()

        fun flushPlain() {
            if (cur.isEmpty()) return
            val s = cur.toString()
            cur.clear()
            // Apply any in-stack composite style and substitute emoji.
            val composed = if (stack.isEmpty()) Style.NULL else compose(stack)
            appendWithEmoji(text, s, composed)
        }

        while (i < n) {
            val ch = input[i]
            when {
                ch == '\\' && i + 1 < n && input[i + 1] == '[' -> {
                    cur.append('[')
                    i += 2
                }
                ch == '[' -> {
                    val close = input.indexOf(']', i + 1)
                    if (close < 0) {
                        cur.append(ch)
                        i += 1
                    } else {
                        flushPlain()
                        val raw = input.substring(i + 1, close).trim()
                        i = close + 1
                        when {
                            raw.isEmpty() -> {
                                // [] → literal
                                cur.append("[]")
                            }
                            raw == "/" -> {
                                if (stack.isNotEmpty()) stack.removeLast()
                            }
                            raw.startsWith("/") -> {
                                val tag = raw.removePrefix("/").trim()
                                // Pop until we find a matching tag (rich-faithful: pop nearest match).
                                val idx = stack.indexOfLast { it.first == tag }
                                if (idx >= 0) {
                                    while (stack.size > idx) stack.removeLast()
                                }
                                // If not found, ignore — same as rich.
                            }
                            else -> {
                                val style = resolveTag(raw, theme)
                                stack += raw to style
                            }
                        }
                    }
                }
                else -> {
                    cur.append(ch)
                    i += 1
                }
            }
        }
        flushPlain()
        return text
    }

    private fun appendWithEmoji(text: Text, s: String, style: Style) {
        var cursor = 0
        for (m in EMOJI_PATTERN.findAll(s)) {
            if (m.range.first > cursor) {
                text.append(s.substring(cursor, m.range.first), if (style.isNull) null else style)
            }
            val name = m.groupValues[1]
            val emoji = EmojiMap[name]
            text.append(emoji ?: m.value, if (style.isNull) null else style)
            cursor = m.range.last + 1
        }
        if (cursor < s.length) {
            text.append(s.substring(cursor), if (style.isNull) null else style)
        }
    }

    private fun resolveTag(tag: String, theme: Theme): Style {
        // Split on whitespace, but keep `link=...` as a single token.
        val tokens = tag.split(Regex("\\s+")).filter { it.isNotEmpty() }
        var result = Style.NULL
        var i = 0
        while (i < tokens.size) {
            val tok = tokens[i]
            // 1. Theme lookup wins for single-word tags.
            val themed = theme[tok]
            if (themed != null) {
                result += themed
                i += 1
                continue
            }
            // 2. link=URL handled separately so URL containing other reserved words doesn't confuse.
            if (tok.startsWith("link=")) {
                result = result.copy(link = tok.removePrefix("link="))
                i += 1
                continue
            }
            // 3. Otherwise consume tokens up to next theme name (greedy) and parse as a style chunk.
            val chunk = StringBuilder(tok)
            i += 1
            while (i < tokens.size) {
                val next = tokens[i]
                if (theme.contains(next) || next.startsWith("link=")) break
                chunk.append(' ').append(next)
                i += 1
            }
            try {
                result += StyleParser.parse(chunk.toString())
            } catch (e: IllegalStateException) {
                // Unknown tag → ignore (rich-faithful). Could log later.
            }
        }
        return result
    }

    private fun compose(stack: ArrayDeque<Pair<String, Style>>): Style {
        var result = Style.NULL
        for ((_, s) in stack) result += s
        return result
    }

    /** Escape a string so its `[` characters are treated as literals when parsed by [parse]. */
    public fun escape(s: String): String = s.replace("[", "\\[")
}
