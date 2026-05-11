package tools.konsole.rich.layout

import tools.konsole.rich.Console
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Style

/** CSS-style 4-side padding. */
public data class Padding(
    public val top: Int = 0,
    public val right: Int = 0,
    public val bottom: Int = 0,
    public val left: Int = 0,
) {
    init {
        require(top >= 0 && right >= 0 && bottom >= 0 && left >= 0) {
            "Padding values must be >= 0, got ($top,$right,$bottom,$left)"
        }
    }

    public val horizontal: Int get() = left + right
    public val vertical: Int get() = top + bottom

    public companion object {
        public val NONE: Padding = Padding(0, 0, 0, 0)

        /** CSS-style 1/2/4-value shorthand. */
        public fun of(vararg values: Int): Padding = when (values.size) {
            0 -> NONE
            1 -> Padding(values[0], values[0], values[0], values[0])
            2 -> Padding(values[0], values[1], values[0], values[1])
            3 -> Padding(values[0], values[1], values[2], values[1])
            4 -> Padding(values[0], values[1], values[2], values[3])
            else -> error("Padding.of() expects 1-4 values, got ${values.size}")
        }
    }
}

/** A renderable that wraps a child with [Padding]. */
public class Padded(
    public val renderable: Renderable,
    public val padding: Padding,
    public val expand: Boolean = false,
    public val style: Style? = null,
) : Measurable {

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> = sequence {
        val outer = options.maxWidth
        val inner = (outer - padding.horizontal).coerceAtLeast(1)
        val childOptions = options.withMaxWidth(inner)
        val targetWidth = if (expand) outer else outer
        val padStyle = style

        // Top padding lines.
        repeat(padding.top) {
            if (padStyle != null) yield(Segment(" ".repeat(targetWidth), padStyle))
            else if (targetWidth > 0) yield(Segment(" ".repeat(targetWidth)))
            yield(Segment.LINE)
        }

        // Buffer child lines so we can pad each side correctly.
        val childLines = collectLines(renderable.render(console, childOptions))
        for ((idx, line) in childLines.withIndex()) {
            if (idx > 0) yield(Segment.LINE)
            // Left pad
            if (padding.left > 0) {
                if (padStyle != null) yield(Segment(" ".repeat(padding.left), padStyle))
                else yield(Segment(" ".repeat(padding.left)))
            }
            // Child segments
            for (s in line.segments) yield(s)
            // Right pad — pad to targetWidth - left - lineWidth
            val rightPad = padding.right + (if (expand) (inner - line.cells).coerceAtLeast(0) else 0)
            if (rightPad > 0) {
                if (padStyle != null) yield(Segment(" ".repeat(rightPad), padStyle))
                else yield(Segment(" ".repeat(rightPad)))
            }
        }

        // Bottom padding lines.
        repeat(padding.bottom) {
            yield(Segment.LINE)
            if (padStyle != null) yield(Segment(" ".repeat(targetWidth), padStyle))
            else if (targetWidth > 0) yield(Segment(" ".repeat(targetWidth)))
        }
    }

    override fun measure(console: Console, options: RenderOptions): Measurement {
        val inner = (options.maxWidth - padding.horizontal).coerceAtLeast(0)
        val childOptions = options.withMaxWidth(inner)
        val child = Measurement.get(console, childOptions, renderable)
        return Measurement(
            minimum = (child.minimum + padding.horizontal).coerceAtMost(options.maxWidth),
            maximum = (child.maximum + padding.horizontal).coerceAtMost(options.maxWidth),
        )
    }
}

/** A line of segments with its visible cell width. */
internal data class CollectedLine(val segments: List<Segment>, val cells: Int)

internal fun collectLines(input: Sequence<Segment>): List<CollectedLine> {
    val lines = mutableListOf<CollectedLine>()
    var current = mutableListOf<Segment>()
    var width = 0
    for (seg in input) {
        if (seg.control is tools.konsole.rich.Control.NewLine) {
            lines += CollectedLine(current, width)
            current = mutableListOf()
            width = 0
        } else {
            current += seg
            width += seg.cellLength
        }
    }
    lines += CollectedLine(current, width)
    return lines
}

/**
 * Word-wrap each line so it fits within [maxWidth]. Long words are hard-broken only when they
 * cannot fit on a fresh line. Used by container layouts (Columns, Panel) to constrain child output.
 */
internal fun wrapLines(lines: List<CollectedLine>, maxWidth: Int): List<CollectedLine> {
    if (maxWidth <= 0) return lines
    val out = mutableListOf<CollectedLine>()
    for (line in lines) {
        if (line.cells <= maxWidth) {
            out += line
            continue
        }
        var current = mutableListOf<Segment>()
        var col = 0
        for (seg in line.segments) {
            var i = 0
            val text = seg.text
            while (i < text.length) {
                val remaining = maxWidth - col
                val available = text.length - i
                if (available <= remaining) {
                    if (available > 0) current += Segment(text.substring(i), seg.style, seg.control)
                    col += available
                    i = text.length
                    continue
                }
                val window = text.substring(i, i + remaining)
                val lastSpace = window.lastIndexOf(' ')
                if (lastSpace > 0) {
                    current += Segment(text.substring(i, i + lastSpace), seg.style, seg.control)
                    col += lastSpace
                    i += lastSpace
                    if (i < text.length && text[i] == ' ') i += 1
                    out += CollectedLine(current, col)
                    current = mutableListOf()
                    col = 0
                } else if (col > 0) {
                    // No whitespace in window and we already have content — break to a fresh
                    // line so the upcoming word stays whole.
                    out += CollectedLine(current, col)
                    current = mutableListOf()
                    col = 0
                } else {
                    // Hard break: word is longer than the entire line.
                    current += Segment(text.substring(i, i + remaining), seg.style, seg.control)
                    col += remaining
                    i += remaining
                    out += CollectedLine(current, col)
                    current = mutableListOf()
                    col = 0
                }
            }
        }
        out += CollectedLine(current, col)
    }
    return out
}
