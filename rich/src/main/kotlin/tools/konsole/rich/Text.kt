package tools.konsole.rich

import tools.konsole.rich.markup.Markup
import tools.konsole.rich.text.Justify
import tools.konsole.rich.text.Overflow

/**
 * A mutable, styled string of text. The closest thing rich-Kotlin has to Python's `Text`.
 *
 * Internally: a [StringBuilder] plus a list of [Span]s. Spans are stored in insertion order,
 * which is also paint order — when spans overlap, later spans win on shared characters.
 *
 * Text instances are mutable and not threadsafe. Most builder methods return `this` for fluent chaining.
 */
public class Text(
    text: String = "",
    public var style: Style = Style.NULL,
    public var justify: Justify = Justify.Default,
    public var overflow: Overflow = Overflow.Fold,
    public var noWrap: Boolean = false,
    public var endChar: String = "",
) : Measurable {

    private val sb: StringBuilder = StringBuilder(text)
    private val spans: MutableList<Span> = mutableListOf()

    public val length: Int get() = sb.length
    public val plain: String get() = sb.toString()
    public fun isEmpty(): Boolean = sb.isEmpty()

    /** Read-only snapshot of the current spans, in paint order. */
    public val spansSnapshot: List<Span> get() = spans.toList()

    /** Append a styled string fragment. */
    public fun append(text: String, style: Style? = null): Text {
        if (text.isEmpty()) return this
        val start = sb.length
        sb.append(text)
        if (style != null && !style.isNull) {
            spans += Span(start, sb.length, style)
        }
        return this
    }

    /** Append another [Text], shifting its spans into our coordinate space. */
    public fun append(other: Text): Text {
        if (other.sb.isEmpty()) return this
        val offset = sb.length
        sb.append(other.sb)
        // Apply the other Text's base style as a bottom-layer span before its own spans.
        if (!other.style.isNull) {
            spans += Span(offset, offset + other.sb.length, other.style)
        }
        for (s in other.spans) {
            spans += Span(s.start + offset, s.end + offset, s.style)
        }
        return this
    }

    /** Apply a style over the half-open range [start, end). */
    public fun stylize(start: Int, end: Int, style: Style): Text {
        if (style.isNull) return this
        val s = start.coerceIn(0, sb.length)
        val e = end.coerceIn(s, sb.length)
        if (s == e) return this
        spans += Span(s, e, style)
        return this
    }

    /** Apply a style to every regex match. */
    public fun highlight(regex: Regex, style: Style): Text {
        for (m in regex.findAll(sb)) stylize(m.range.first, m.range.last + 1, style)
        return this
    }

    /** Pad the text on the right to [width] columns. */
    public fun pad(width: Int, char: Char = ' '): Text {
        val needed = width - sb.length
        if (needed > 0) sb.append(char.toString().repeat(needed))
        return this
    }

    /** Split into multiple Texts on a separator (preserves spans within each piece). */
    public fun split(sep: String = "\n"): List<Text> {
        if (sep.isEmpty()) return listOf(this)
        val out = mutableListOf<Text>()
        var cursor = 0
        while (cursor <= sb.length) {
            val idx = sb.indexOf(sep, cursor)
            val end = if (idx < 0) sb.length else idx
            val piece = sliceText(cursor, end)
            out += piece
            if (idx < 0) break
            cursor = idx + sep.length
        }
        return out
    }

    private fun sliceText(start: Int, end: Int): Text {
        val piece = Text(
            text = sb.substring(start, end),
            style = style,
            justify = justify,
            overflow = overflow,
            noWrap = noWrap,
        )
        for (s in spans) {
            val ss = s.start.coerceAtLeast(start)
            val se = s.end.coerceAtMost(end)
            if (ss < se) piece.spans += Span(ss - start, se - start, s.style)
        }
        return piece
    }

    /** Take a snapshot copy (for thread-safe handoff). */
    public fun copy(): Text {
        val out = Text(
            text = sb.toString(),
            style = style,
            justify = justify,
            overflow = overflow,
            noWrap = noWrap,
        )
        out.spans += spans
        return out
    }

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> = buildList {
        val plain = sb.toString()
        if (plain.isEmpty()) {
            if (endChar.isNotEmpty()) appendSplitOnNewlines(endChar, style)
            return@buildList
        }
        val n = plain.length
        val perChar = arrayOfNulls<Style>(n)
        if (!style.isNull) for (i in 0 until n) perChar[i] = style
        for (s in spans) {
            for (i in s.start until s.end) {
                perChar[i] = (perChar[i] ?: Style.NULL) + s.style
            }
        }
        // Coalesce consecutive same-style runs into segments. Split on '\n'.
        var runStart = 0
        var i = 0
        while (i < n) {
            val ch = plain[i]
            if (ch == '\n') {
                if (i > runStart) {
                    add(Segment(plain.substring(runStart, i), perChar[runStart]))
                }
                add(Segment.LINE)
                runStart = i + 1
                i = runStart
                continue
            }
            if (i > runStart && perChar[i] != perChar[i - 1]) {
                add(Segment(plain.substring(runStart, i), perChar[runStart]))
                runStart = i
            }
            i += 1
        }
        if (runStart < n) add(Segment(plain.substring(runStart, n), perChar[runStart]))
        if (endChar.isNotEmpty()) appendSplitOnNewlines(endChar, style)
    }.asSequence()

    private fun MutableList<Segment>.appendSplitOnNewlines(s: String, style: Style) {
        var start = 0
        var idx = s.indexOf('\n')
        while (idx >= 0) {
            if (idx > start) add(Segment(s.substring(start, idx), if (style.isNull) null else style))
            add(Segment.LINE)
            start = idx + 1
            idx = s.indexOf('\n', start)
        }
        if (start < s.length) add(Segment(s.substring(start), if (style.isNull) null else style))
    }

    override fun measure(console: Console, options: RenderOptions): Measurement {
        val plain = sb.toString()
        if (plain.isEmpty()) return Measurement.ZERO
        var longestLine = 0
        var longestToken = 0
        var lineLen = 0
        var tokenLen = 0
        for (ch in plain) {
            if (ch == '\n') {
                if (lineLen > longestLine) longestLine = lineLen
                if (tokenLen > longestToken) longestToken = tokenLen
                lineLen = 0; tokenLen = 0
            } else {
                lineLen += 1
                if (ch.isWhitespace()) {
                    if (tokenLen > longestToken) longestToken = tokenLen
                    tokenLen = 0
                } else tokenLen += 1
            }
        }
        if (lineLen > longestLine) longestLine = lineLen
        if (tokenLen > longestToken) longestToken = tokenLen
        val min = if (noWrap) longestLine else longestToken
        val max = longestLine
        return Measurement(min.coerceAtMost(options.maxWidth), maxOf(min, max).coerceAtMost(options.maxWidth))
    }

    override fun toString(): String = sb.toString()

    public companion object {
        public fun fromMarkup(s: String, theme: Theme = Theme.DEFAULT): Text =
            Markup.parse(s, theme)

        public fun assemble(vararg parts: Pair<String, Style?>): Text {
            val out = Text()
            for ((text, style) in parts) out.append(text, style)
            return out
        }
    }
}

/** A styled span over a [Text]'s underlying StringBuilder. */
public data class Span(public val start: Int, public val end: Int, public val style: Style) {
    init {
        require(start >= 0) { "start must be >= 0, got $start" }
        require(end >= start) { "end ($end) must be >= start ($start)" }
    }
}

/**
 * A renderable that re-emits a string already containing ANSI escape sequences verbatim.
 *
 * Used to preserve konsole-core [tools.konsole.core.style.StyledContent] output,
 * which is already serialized SGR + content + reset.
 */
public class RawAnsi(public val ansi: String) : Renderable {
    override fun render(console: Console, options: RenderOptions): Sequence<Segment> = buildList {
        var start = 0
        var idx = ansi.indexOf('\n')
        while (idx >= 0) {
            if (idx > start) add(Segment(ansi.substring(start, idx), null))
            add(Segment.LINE)
            start = idx + 1
            idx = ansi.indexOf('\n', start)
        }
        if (start < ansi.length) add(Segment(ansi.substring(start), null))
    }.asSequence()
}
