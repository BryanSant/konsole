package tools.konsole.rich.table

import tools.konsole.rich.Console
import tools.konsole.rich.Control
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Style
import tools.konsole.rich.Text
import tools.konsole.rich.box.Box
import tools.konsole.rich.layout.Padding
import tools.konsole.rich.layout.collectLines
import tools.konsole.rich.markup.Markup
import tools.konsole.rich.text.Justify
import tools.konsole.rich.text.Overflow

/**
 * A grid of cells with optional borders, header, footer, title, and caption.
 * Mirrors `rich.table.Table` with the same width-budgeting semantics.
 */
public class Table(
    public val title: Renderable? = null,
    public val titleStyle: Style? = null,
    public val titleJustify: Justify = Justify.Center,
    public val caption: Renderable? = null,
    public val captionStyle: Style? = null,
    public val captionJustify: Justify = Justify.Center,
    public val box: Box? = Box.HEAVY_HEAD,
    public val safeBox: Boolean = true,
    public val padding: Padding = Padding(0, 1, 0, 1),
    public val collapsePadding: Boolean = false,
    public val padEdge: Boolean = true,
    public val expand: Boolean = false,
    public val showHeader: Boolean = true,
    public val showFooter: Boolean = false,
    public val showEdge: Boolean = true,
    public val showLines: Boolean = false,
    public val leading: Int = 0,
    public val style: Style = Style.NULL,
    public val rowStyles: List<Style> = emptyList(),
    public val headerStyle: Style? = null,
    public val footerStyle: Style? = null,
    public val borderStyle: Style? = null,
    public val width: Int? = null,
    public val minWidth: Int? = null,
) : Measurable {

    private val columns: MutableList<Column> = mutableListOf()
    private val rows: MutableList<Row> = mutableListOf()

    public val columnCount: Int get() = columns.size
    public val rowCount: Int get() = rows.size

    public fun addColumn(column: Column): Table = apply { columns += column }

    public fun addColumn(
        header: String,
        justify: Justify = Justify.Left,
        style: Style? = null,
        headerStyle: Style? = null,
        footerStyle: Style? = null,
        footer: String? = null,
        width: Int? = null,
        ratio: Int? = null,
        noWrap: Boolean = false,
        overflow: Overflow = Overflow.Ellipsis,
    ): Table = addColumn(
        Column(
            header = Markup.parse(header),
            footer = footer?.let { Markup.parse(it) },
            headerStyle = headerStyle,
            footerStyle = footerStyle,
            style = style,
            justify = justify,
            width = width,
            ratio = ratio,
            noWrap = noWrap,
            overflow = overflow,
        )
    )

    public fun addRow(vararg cells: Any?, style: Style? = null, endSection: Boolean = false): Table {
        // Auto-create columns if rows arrive before columns are declared.
        while (columns.size < cells.size) columns += Column()
        for ((i, cell) in cells.withIndex()) {
            val r: Renderable = when (cell) {
                null -> Text("")
                is Renderable -> cell
                is String -> Markup.parse(cell)
                else -> Text(cell.toString())
            }
            columns[i].cells += r
        }
        // Pad missing cells in fewer-than-column rows.
        for (i in cells.size until columns.size) columns[i].cells += Text("")
        rows += Row(style = style, endSection = endSection)
        return this
    }

    public fun addSection(): Table {
        if (rows.isNotEmpty()) {
            val last = rows.removeLast()
            rows += last.copy(endSection = true)
        }
        return this
    }

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> = sequence {
        if (columns.isEmpty()) return@sequence
        val outerWidth = (width ?: options.maxWidth).coerceAtMost(options.maxWidth).coerceAtLeast(1)
        val widths = computeColumnWidths(console, options, outerWidth)
        val totalInner = widths.sum() + ((columns.size - 1) * if (box == null) 1 else 1)
        val totalOuter = if (box != null && showEdge) totalInner + 2 else totalInner

        val borderS = borderStyle ?: if (style.isNull) null else style
        val box = box

        // Title.
        if (title != null) {
            val rendered = renderInline(title, options.withMaxWidth(totalOuter))
            yieldAll(centeredSingleLine(rendered, totalOuter, titleJustify, titleStyle))
            yield(Segment.LINE)
        }

        // Top border.
        if (box != null && showEdge) {
            yield(Segment(box.getTop(widths), borderS))
            yield(Segment.LINE)
        }

        // Header.
        if (showHeader && columns.any { it.header != null }) {
            val cells = columns.map { it.header ?: Text("") }
            val resolvedHeaderStyle = headerStyle ?: console.theme["table.header"] ?: Style(bold = true)
            for (line in renderRowLines(console, options, cells, widths, headerOverride = true, defaultStyle = resolvedHeaderStyle)) {
                yieldAll(line)
                yield(Segment.LINE)
            }
            if (box != null) {
                yield(Segment(box.getHeadRow(widths), borderS))
                yield(Segment.LINE)
            }
        }

        // Body rows.
        for (rowIdx in 0 until rowCount) {
            val cellRenderables = columns.map { it.cells.getOrNull(rowIdx) ?: Text("") }
            val baseRowStyle = rows[rowIdx].style
                ?: rowStyles.getOrNull(rowIdx % rowStyles.size.coerceAtLeast(1)).takeIf { rowStyles.isNotEmpty() }
            val lines = renderRowLines(console, options, cellRenderables, widths, headerOverride = false, defaultStyle = baseRowStyle)
            for (line in lines) {
                yieldAll(line)
                yield(Segment.LINE)
            }
            // Section divider or per-row line.
            val isLast = rowIdx == rowCount - 1
            if (!isLast) {
                if (rows[rowIdx].endSection || showLines) {
                    if (box != null) {
                        yield(Segment(box.getRow(widths), borderS))
                        yield(Segment.LINE)
                    }
                }
            }
        }

        // Footer.
        if (showFooter && columns.any { it.footer != null }) {
            if (box != null) {
                yield(Segment(box.getFootRow(widths), borderS))
                yield(Segment.LINE)
            }
            val cells = columns.map { it.footer ?: Text("") }
            val resolvedFooterStyle = footerStyle ?: console.theme["table.footer"] ?: Style(bold = true)
            for (line in renderRowLines(console, options, cells, widths, headerOverride = true, defaultStyle = resolvedFooterStyle)) {
                yieldAll(line)
                yield(Segment.LINE)
            }
        }

        // Bottom border.
        if (box != null && showEdge) {
            yield(Segment(box.getBottom(widths), borderS))
        }

        // Caption.
        if (caption != null) {
            yield(Segment.LINE)
            val rendered = renderInline(caption, options.withMaxWidth(totalOuter))
            yieldAll(centeredSingleLine(rendered, totalOuter, captionJustify, captionStyle))
        }
    }

    private fun centeredSingleLine(text: Pair<List<Segment>, Int>, totalWidth: Int, justify: Justify, style: Style?): Sequence<Segment> = sequence {
        val (segs, cells) = text
        val pad = (totalWidth - cells).coerceAtLeast(0)
        val (left, right) = when (justify) {
            Justify.Right -> pad to 0
            Justify.Left, Justify.Default, Justify.Full -> 0 to pad
            Justify.Center -> {
                val l = pad / 2
                l to (pad - l)
            }
        }
        if (left > 0) yield(Segment(" ".repeat(left), style))
        if (style != null) for (s in segs) yield(if (s.style == null) Segment(s.text, style) else s)
        else for (s in segs) yield(s)
        if (right > 0) yield(Segment(" ".repeat(right), style))
    }

    private fun renderInline(r: Renderable, options: RenderOptions): Pair<List<Segment>, Int> {
        val out = mutableListOf<Segment>()
        var cells = 0
        for (s in r.render(Console.string(width = options.maxWidth, colorSystem = tools.konsole.core.ColorSystem.TrueColor), options)) {
            if (s.control is Control.NewLine) break
            if (s.control != null) continue
            out += s
            cells += s.cellLength
        }
        return out to cells
    }

    /** Produce the horizontal lines for a single logical row, including borders if box != null. */
    private fun renderRowLines(
        console: Console,
        options: RenderOptions,
        cells: List<Renderable>,
        widths: List<Int>,
        headerOverride: Boolean,
        defaultStyle: Style?,
    ): List<Sequence<Segment>> {
        // Render each cell, applying padding inside its column width.
        val cellLines: List<List<tools.konsole.rich.layout.CollectedLine>> = cells.mapIndexed { i, cell ->
            val col = columns[i]
            val padHoriz = padding.horizontal
            val cellWidth = (widths[i] - padHoriz).coerceAtLeast(1)
            val lines = collectLines(cell.render(console, options.update(maxWidth = cellWidth, overflow = col.overflow, noWrap = col.noWrap)))
            lines
        }
        val maxLines = cellLines.maxOf { it.size }
        val borderS = borderStyle ?: if (style.isNull) null else style
        val out = mutableListOf<Sequence<Segment>>()
        for (lineIdx in 0 until maxLines) {
            val line = sequence {
                if (box != null && showEdge) yield(Segment(box.midLeft.toString(), borderS))
                for ((i, lines) in cellLines.withIndex()) {
                    if (i > 0 && box != null) yield(Segment(box.midDivider.toString(), borderS))
                    val col = columns[i]
                    val cellLine = lines.getOrNull(lineIdx)
                    val cellW = widths[i] - padding.horizontal
                    val resolvedJustify = col.justify
                    val cellStyle = col.style ?: defaultStyle
                    if (padding.left > 0) yield(if (cellStyle != null) Segment(" ".repeat(padding.left), cellStyle) else Segment(" ".repeat(padding.left)))
                    if (cellLine == null) {
                        // Empty padding row
                        if (cellStyle != null) yield(Segment(" ".repeat(cellW.coerceAtLeast(0)), cellStyle))
                        else yield(Segment(" ".repeat(cellW.coerceAtLeast(0))))
                    } else {
                        val totalCells = cellLine.cells
                        val leftover = (cellW - totalCells).coerceAtLeast(0)
                        val (lp, rp) = when (resolvedJustify) {
                            Justify.Right -> leftover to 0
                            Justify.Center -> {
                                val l = leftover / 2
                                l to (leftover - l)
                            }
                            else -> 0 to leftover
                        }
                        if (lp > 0) yield(if (cellStyle != null) Segment(" ".repeat(lp), cellStyle) else Segment(" ".repeat(lp)))
                        if (cellStyle != null) {
                            for (s in cellLine.segments) yield(if (s.style == null) Segment(s.text, cellStyle) else s)
                        } else {
                            for (s in cellLine.segments) yield(s)
                        }
                        if (rp > 0) yield(if (cellStyle != null) Segment(" ".repeat(rp), cellStyle) else Segment(" ".repeat(rp)))
                    }
                    if (padding.right > 0) yield(if (cellStyle != null) Segment(" ".repeat(padding.right), cellStyle) else Segment(" ".repeat(padding.right)))
                }
                if (box != null && showEdge) yield(Segment(box.midRight.toString(), borderS))
            }
            out += line
        }
        return out
    }

    private fun computeColumnWidths(console: Console, options: RenderOptions, outerWidth: Int): List<Int> {
        val n = columns.size
        if (n == 0) return emptyList()
        val borderOverhead = if (box != null && showEdge) 2 else 0
        val dividerOverhead = if (box != null) (n - 1) else 0
        val available = (outerWidth - borderOverhead - dividerOverhead).coerceAtLeast(n)
        // For each column, get the natural width (max of all cells + header + footer measurement).
        val natural = IntArray(n)
        for ((i, col) in columns.withIndex()) {
            val pieces = mutableListOf<Renderable?>()
            if (showHeader) pieces += col.header
            if (showFooter) pieces += col.footer
            pieces += col.cells
            var maxNeeded = 0
            for (p in pieces) {
                if (p == null) continue
                val m = Measurement.get(console, options.withMaxWidth(available), p)
                if (m.maximum > maxNeeded) maxNeeded = m.maximum
            }
            natural[i] = maxNeeded + padding.horizontal
        }

        // Apply explicit widths first.
        val widths = IntArray(n)
        var fixedTotal = 0
        var ratioSum = 0
        val unbounded = mutableListOf<Int>()
        for ((i, col) in columns.withIndex()) {
            when {
                col.width != null -> {
                    widths[i] = col.width.coerceIn(1, available)
                    fixedTotal += widths[i]
                }
                col.ratio != null -> {
                    ratioSum += col.ratio
                    unbounded += i
                }
                else -> {
                    widths[i] = natural[i].coerceIn(col.minWidth ?: 1, col.maxWidth ?: available)
                    fixedTotal += widths[i]
                }
            }
        }
        // Distribute leftover among ratio columns.
        var leftover = (available - fixedTotal).coerceAtLeast(0)
        if (ratioSum > 0) {
            for (i in unbounded) {
                val col = columns[i]
                val share = (leftover * (col.ratio ?: 0)) / ratioSum
                widths[i] = share.coerceAtLeast(col.minWidth ?: 1)
            }
        }

        // If expand=true, distribute any remaining leftover to columns proportional to natural widths.
        var current = widths.sum()
        if (expand && current < available) {
            val grow = available - current
            // Spread evenly.
            for (j in 0 until grow) {
                widths[j % n] += 1
            }
        }

        // If overflow (columns need more than available), shrink starting from widest.
        current = widths.sum()
        while (current > available) {
            val widest = (0 until n).maxBy { widths[it] }
            if (widths[widest] <= 1) break
            widths[widest] -= 1
            current -= 1
        }
        return widths.toList()
    }

    override fun measure(console: Console, options: RenderOptions): Measurement {
        if (columns.isEmpty()) return Measurement.ZERO
        val widths = computeColumnWidths(console, options, options.maxWidth)
        val total = widths.sum() + (if (box != null && showEdge) 2 else 0) + (if (box != null) columns.size - 1 else 0)
        return Measurement(total.coerceAtMost(options.maxWidth), total.coerceAtMost(options.maxWidth))
    }
}

/** Convenience grid (no box border) for arranging renderables in rows/cols. */
public fun grid(vararg columns: Column = arrayOf()): Table = Table(box = null, showEdge = false, padding = Padding.NONE).apply {
    for (c in columns) addColumn(c)
}
