package tools.konsole.rich.layout

import tools.konsole.rich.Console
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Style
import tools.konsole.rich.text.Justify

public enum class VerticalAlign { Top, Middle, Bottom }

/**
 * Aligns a child renderable horizontally (and optionally vertically) within a fixed width.
 *
 * Mirrors `rich.align.Align` — use [left], [center], [right] factories or the constructor.
 */
public class Align(
    public val renderable: Renderable,
    public val align: Justify = Justify.Center,
    public val vertical: VerticalAlign = VerticalAlign.Top,
    public val style: Style? = null,
    public val pad: Boolean = true,
    public val width: Int? = null,
    public val height: Int? = null,
) : Measurable {

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> = buildList {
        val outerWidth = (width ?: options.maxWidth).coerceAtMost(options.maxWidth).coerceAtLeast(0)
        val targetHeight = height
        val childOpts = options.withMaxWidth(outerWidth)
        val lines = collectLines(renderable.render(console, childOpts))
        val totalLines = lines.size
        val (topPad, bottomPad) = if (targetHeight != null && targetHeight > totalLines) {
            val pad = targetHeight - totalLines
            when (vertical) {
                VerticalAlign.Top -> 0 to pad
                VerticalAlign.Bottom -> pad to 0
                VerticalAlign.Middle -> {
                    val t = pad / 2
                    t to (pad - t)
                }
            }
        } else 0 to 0

        val padStyle = style

        repeat(topPad) {
            if (pad && outerWidth > 0) {
                if (padStyle != null) add(Segment(" ".repeat(outerWidth), padStyle))
                else add(Segment(" ".repeat(outerWidth)))
            }
            add(Segment.LINE)
        }

        for ((idx, line) in lines.withIndex()) {
            if (idx > 0) add(Segment.LINE)
            val leftover = (outerWidth - line.cells).coerceAtLeast(0)
            val (leftPad, rightPad) = when (align) {
                Justify.Default, Justify.Left, Justify.Full -> 0 to leftover
                Justify.Right -> leftover to 0
                Justify.Center -> {
                    val l = leftover / 2
                    l to (leftover - l)
                }
            }
            if (leftPad > 0) add(if (padStyle != null) Segment(" ".repeat(leftPad), padStyle) else Segment(" ".repeat(leftPad)))
            for (s in line.segments) add(s)
            if (pad && rightPad > 0) add(if (padStyle != null) Segment(" ".repeat(rightPad), padStyle) else Segment(" ".repeat(rightPad)))
        }

        repeat(bottomPad) {
            add(Segment.LINE)
            if (pad && outerWidth > 0) {
                if (padStyle != null) add(Segment(" ".repeat(outerWidth), padStyle))
                else add(Segment(" ".repeat(outerWidth)))
            }
        }
    }.asSequence()

    override fun measure(console: Console, options: RenderOptions): Measurement {
        val w = width ?: options.maxWidth
        return Measurement(w.coerceAtMost(options.maxWidth), w.coerceAtMost(options.maxWidth))
    }

    public companion object {
        public fun left(renderable: Renderable, vertical: VerticalAlign = VerticalAlign.Top, style: Style? = null, pad: Boolean = true, width: Int? = null, height: Int? = null): Align =
            Align(renderable, Justify.Left, vertical, style, pad, width, height)

        public fun center(renderable: Renderable, vertical: VerticalAlign = VerticalAlign.Top, style: Style? = null, pad: Boolean = true, width: Int? = null, height: Int? = null): Align =
            Align(renderable, Justify.Center, vertical, style, pad, width, height)

        public fun right(renderable: Renderable, vertical: VerticalAlign = VerticalAlign.Top, style: Style? = null, pad: Boolean = true, width: Int? = null, height: Int? = null): Align =
            Align(renderable, Justify.Right, vertical, style, pad, width, height)
    }
}
