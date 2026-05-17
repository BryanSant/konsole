package tools.konsole.rich.layout

import tools.konsole.rich.Console
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.text.Justify

/**
 * Renders multiple [Renderable]s side-by-side, wrapping into a grid that fits the available width.
 * Mirrors `rich.columns.Columns`.
 */
public class Columns(
    public val renderables: List<Renderable>,
    public val padding: Int = 1,
    public val width: Int? = null,
    public val expand: Boolean = false,
    public val equal: Boolean = false,
    public val align: Justify = Justify.Left,
    public val titleStyle: tools.konsole.rich.Style? = null,
) : Measurable {

    public constructor(vararg renderables: Renderable) : this(renderables.toList())

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> = buildList {
        if (renderables.isEmpty()) return@buildList
        val total = options.maxWidth.coerceAtLeast(1)
        val cellWidth = when {
            width != null -> width
            equal -> ((total - padding * (renderables.size - 1)) / renderables.size).coerceAtLeast(1)
            else -> renderables.map { Measurement.get(console, options, it) }
                .maxOf { it.maximum }
                .coerceAtLeast(1)
        }
        val effectiveCellWidth = cellWidth.coerceAtMost(total)
        val perRow = if (equal) {
            renderables.size
        } else {
            ((total + padding) / (effectiveCellWidth + padding)).coerceAtLeast(1)
        }
        val grid = renderables.chunked(perRow)
        for ((rowIdx, row) in grid.withIndex()) {
            if (rowIdx > 0) add(Segment.LINE)
            val rendered = row.map {
                wrapLines(collectLines(it.render(console, options.withMaxWidth(effectiveCellWidth))), effectiveCellWidth)
            }
            val height = rendered.maxOf { it.size }
            for (lineIdx in 0 until height) {
                if (lineIdx > 0) add(Segment.LINE)
                for ((colIdx, lines) in rendered.withIndex()) {
                    if (colIdx > 0) add(Segment(" ".repeat(padding)))
                    val line = lines.getOrNull(lineIdx)
                    if (line == null) {
                        add(Segment(" ".repeat(effectiveCellWidth)))
                    } else {
                        val pad = (effectiveCellWidth - line.cells).coerceAtLeast(0)
                        val (l, r) = when (align) {
                            Justify.Right -> pad to 0
                            Justify.Center -> {
                                val lp = pad / 2
                                lp to (pad - lp)
                            }
                            else -> 0 to pad
                        }
                        if (l > 0) add(Segment(" ".repeat(l)))
                        for (s in line.segments) add(s)
                        if (r > 0) add(Segment(" ".repeat(r)))
                    }
                }
            }
        }
    }.asSequence()

    override fun measure(console: Console, options: RenderOptions): Measurement {
        if (renderables.isEmpty()) return Measurement.ZERO
        val maxes = renderables.map { Measurement.get(console, options, it).maximum }
        val sum = maxes.sum() + padding * (renderables.size - 1)
        val largest = maxes.max()
        return Measurement(largest.coerceAtMost(options.maxWidth), sum.coerceAtMost(options.maxWidth))
    }
}
