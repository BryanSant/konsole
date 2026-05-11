package tools.konsole.rich.layout

import tools.konsole.rich.Console
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Text

/**
 * A recursive splittable region. Mirrors `rich.layout.Layout`.
 *
 * A leaf layout has [renderable] content; a branch layout has [children] split horizontally
 * (via [splitRow]) or vertically (via [splitColumn]).
 *
 * Per-child sizing:
 *   - [size]: fixed cell count (lines for vertical, columns for horizontal)
 *   - [ratio]: proportional share of remaining space
 *   - [minimumSize]: floor for the computed size
 *   - [visible]: false hides the child
 */
public class Layout(
    public var name: String? = null,
    public var renderable: Renderable = Text(""),
    public var ratio: Int = 1,
    public var size: Int? = null,
    public var minimumSize: Int = 1,
    public var visible: Boolean = true,
) : Measurable {

    /** Direction the children are arranged in (only set on a branch). */
    public enum class Direction { Row, Column }

    private val _children: MutableList<Layout> = mutableListOf()
    public val children: List<Layout> get() = _children
    public var direction: Direction? = null
        private set

    /** Replace this layout's content. */
    public fun update(renderable: Renderable): Layout = apply { this.renderable = renderable }
    public fun update(text: String): Layout = update(Text(text))

    /** Split this layout horizontally into the given children. */
    public fun splitRow(vararg children: Layout): Layout = apply {
        direction = Direction.Row
        _children.clear(); _children.addAll(children)
    }

    /** Split this layout vertically into the given children. */
    public fun splitColumn(vararg children: Layout): Layout = apply {
        direction = Direction.Column
        _children.clear(); _children.addAll(children)
    }

    /** Look up a sub-layout by [name]. */
    public operator fun get(name: String): Layout? {
        if (this.name == name) return this
        for (child in _children) {
            val found = child[name]
            if (found != null) return found
        }
        return null
    }

    private fun isBranch(): Boolean = direction != null && _children.any { it.visible }

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> = sequence {
        val height = options.height ?: console.height
        val rows: List<List<CollectedLine>> = renderRegion(console, options.maxWidth, height)
        for ((idx, row) in rows.withIndex()) {
            if (idx > 0) yield(Segment.LINE)
            for (s in row.flatMap { it.segments }) yield(s)
        }
    }

    /** Render this layout into [width] x [height] cells; returns one [CollectedLine] per output row. */
    private fun renderRegion(console: Console, width: Int, height: Int): List<List<CollectedLine>> {
        if (!visible) return emptyList()
        if (!isBranch()) {
            val opts = RenderOptions(maxWidth = width, height = height, highlight = true, markup = true)
            val lines = collectLines(renderable.render(console, opts))
            // Pad/truncate to height
            val out = mutableListOf<List<CollectedLine>>()
            for (i in 0 until height) {
                val line = lines.getOrNull(i)
                if (line == null) {
                    out += listOf(CollectedLine(listOf(Segment(" ".repeat(width))), width))
                } else {
                    val padded = if (line.cells < width) {
                        line.copy(segments = line.segments + Segment(" ".repeat(width - line.cells)), cells = width)
                    } else line
                    out += listOf(padded)
                }
            }
            return out
        }
        val visibleChildren = _children.filter { it.visible }
        return when (direction) {
            Direction.Row -> renderRow(console, width, height, visibleChildren)
            Direction.Column -> renderColumn(console, width, height, visibleChildren)
            null -> emptyList()
        }
    }

    private fun renderRow(console: Console, width: Int, height: Int, children: List<Layout>): List<List<CollectedLine>> {
        val widths = budget(children, width)
        val rendered: List<List<List<CollectedLine>>> = children.mapIndexed { i, c ->
            c.renderRegion(console, widths[i], height)
        }
        val out = mutableListOf<List<CollectedLine>>()
        for (rowIdx in 0 until height) {
            val combined = mutableListOf<CollectedLine>()
            for (childRow in rendered) {
                val line = childRow.getOrNull(rowIdx)?.firstOrNull()
                    ?: CollectedLine(listOf(Segment(" ".repeat(widths[rendered.indexOf(childRow)]))), widths[rendered.indexOf(childRow)])
                combined += line
            }
            out += combined
        }
        return out
    }

    private fun renderColumn(console: Console, width: Int, height: Int, children: List<Layout>): List<List<CollectedLine>> {
        val heights = budget(children, height)
        val out = mutableListOf<List<CollectedLine>>()
        for ((i, c) in children.withIndex()) {
            val rendered = c.renderRegion(console, width, heights[i])
            for (line in rendered) out += line
        }
        return out
    }

    /** Budget child sizes given the available total. Fixed [size] wins over ratio. */
    private fun budget(children: List<Layout>, total: Int): IntArray {
        val n = children.size
        val out = IntArray(n)
        var remaining = total
        var ratioTotal = 0
        for ((i, c) in children.withIndex()) {
            if (c.size != null) {
                out[i] = c.size!!.coerceAtLeast(c.minimumSize).coerceAtMost(remaining)
                remaining -= out[i]
            } else {
                ratioTotal += c.ratio.coerceAtLeast(1)
            }
        }
        if (ratioTotal > 0 && remaining > 0) {
            for ((i, c) in children.withIndex()) {
                if (c.size != null) continue
                val share = (remaining.toDouble() * c.ratio.coerceAtLeast(1) / ratioTotal).toInt()
                out[i] = share.coerceAtLeast(c.minimumSize)
            }
            // Distribute rounding remainder to the first ratio'd child.
            val used = out.sum()
            val leftover = total - used
            if (leftover != 0) {
                for ((i, c) in children.withIndex()) {
                    if (c.size == null) {
                        out[i] = (out[i] + leftover).coerceAtLeast(c.minimumSize)
                        break
                    }
                }
            }
        }
        return out
    }

    override fun measure(console: Console, options: RenderOptions): Measurement =
        Measurement(options.maxWidth, options.maxWidth)
}
