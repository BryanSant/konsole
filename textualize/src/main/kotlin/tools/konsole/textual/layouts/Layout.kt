package tools.konsole.textual.layouts

import tools.konsole.rich.geometry.Region
import tools.konsole.rich.geometry.Spacing
import tools.konsole.textual.css.LayoutKind
import tools.konsole.textual.css.LengthUnit
import tools.konsole.textual.css.Scalar
import tools.konsole.textual.css.Styles
import tools.konsole.textual.widget.Widget

/** Result of laying out one widget — its assigned region. */
public data class Placement(public val widget: Widget, public val region: Region)

/**
 * Common interface for layout engines. Mirrors Python textual's `layouts/`.
 *
 * Given a parent [region] and a list of children with their resolved [Styles],
 * produce a [Placement] for each child describing its rectangle. Children that
 * have `display: none` are emitted with `region = Region.EMPTY`.
 */
public fun interface Layout {
    public fun arrange(region: Region, children: List<Pair<Widget, Styles>>): List<Placement>

    public companion object {
        /** Dispatch to the appropriate engine based on the parent's resolved [layout]. */
        public fun forKind(kind: LayoutKind?): Layout = when (kind) {
            null, LayoutKind.Vertical -> VerticalLayout
            LayoutKind.Horizontal -> HorizontalLayout
            LayoutKind.Grid -> GridLayout
        }
    }
}

/** Top-to-bottom stack. Each child takes the full width (minus margins) and a row count
 *  derived from its `height` Scalar. `auto`/null → "fit to one row" for now. */
public object VerticalLayout : Layout {
    override fun arrange(region: Region, children: List<Pair<Widget, Styles>>): List<Placement> {
        val visible = children.filter { (_, s) -> s.display != tools.konsole.textual.css.Display.None }
        // Determine the available height after summing fixed heights and counting fractions
        val explicit = visible.map { (_, s) -> s.height }
        val totalFractions = explicit.filterNotNull().filter { it.isFraction }.sumOf { it.value }
        val fixedHeights = explicit.map { sc -> if (sc != null && !sc.isFraction && !sc.isAuto) sc.resolve(region.height, 1.0) else 0 }
        val fixedSum = fixedHeights.sum()
        val remaining = (region.height - fixedSum).coerceAtLeast(0)

        val placements = mutableListOf<Placement>()
        var y = region.y
        for ((i, pair) in visible.withIndex()) {
            val (widget, styles) = pair
            val widthScalar = styles.width
            val w = widthScalar?.resolve(region.width, 1.0)?.coerceAtLeast(0) ?: region.width
            val h = when {
                styles.height == null || styles.height.isAuto -> 1
                styles.height.isFraction -> styles.height.resolve(remaining, totalFractions).coerceAtLeast(0)
                else -> fixedHeights[i]
            }
            placements += Placement(widget, Region(region.x, y, w, h))
            y += h
            if (y >= region.bottom) break
        }
        return placements
    }
}

/** Left-to-right stack. Mirror of [VerticalLayout] on the horizontal axis. */
public object HorizontalLayout : Layout {
    override fun arrange(region: Region, children: List<Pair<Widget, Styles>>): List<Placement> {
        val visible = children.filter { (_, s) -> s.display != tools.konsole.textual.css.Display.None }
        val explicit = visible.map { (_, s) -> s.width }
        val totalFractions = explicit.filterNotNull().filter { it.isFraction }.sumOf { it.value }
        val fixedWidths = explicit.map { sc -> if (sc != null && !sc.isFraction && !sc.isAuto) sc.resolve(region.width, 1.0) else 0 }
        val fixedSum = fixedWidths.sum()
        val remaining = (region.width - fixedSum).coerceAtLeast(0)

        val placements = mutableListOf<Placement>()
        var x = region.x
        for ((i, pair) in visible.withIndex()) {
            val (widget, styles) = pair
            val heightScalar = styles.height
            val h = heightScalar?.resolve(region.height, 1.0)?.coerceAtLeast(0) ?: region.height
            val w = when {
                styles.width == null || styles.width.isAuto -> region.width / visible.size.coerceAtLeast(1)
                styles.width.isFraction -> styles.width.resolve(remaining, totalFractions).coerceAtLeast(0)
                else -> fixedWidths[i]
            }
            placements += Placement(widget, Region(x, region.y, w, h))
            x += w
            if (x >= region.right) break
        }
        return placements
    }
}

/**
 * Grid layout — places children in a fixed `(cols × rows)` grid.
 *
 * The parent's [Styles.gridSize] gives the grid dimensions. Track sizes come
 * from [Styles.gridColumns] / [Styles.gridRows] (lists of [Scalar]); if a list
 * is shorter than the grid, the last entry repeats. If not specified, all
 * tracks default to `1fr`. Gaps come from [Styles.gridGutter] (horizontal,
 * vertical), default `0 0`.
 *
 * Children are placed in row-major order. A child can span multiple cells via
 * its own [Styles.columnSpan] / [Styles.rowSpan] (default 1). If a child's
 * span doesn't fit in the remaining cells of the current row, it wraps to the
 * next row (the textual behaviour). Children beyond the declared grid extend
 * into additional rows.
 *
 * The layout is implemented in two passes:
 *   1. Cell assignment — walk children, find the next free slot that fits.
 *   2. Track resolution — compute pixel widths for columns then rows, then
 *      convert each child's cell rectangle into a screen [Region].
 */
public object GridLayout : Layout {

    override fun arrange(region: Region, children: List<Pair<Widget, Styles>>): List<Placement> {
        if (children.isEmpty()) return emptyList()
        // The grid metadata lives on the *parent*; arrange() receives only the
        // children's styles. Look for the first child whose parent passed grid
        // metadata through — by convention the caller (Compositor) merges the
        // parent's grid-* properties into a sentinel record. We don't have that
        // sentinel yet, so for now read from the FIRST child only as a fallback
        // when callers don't pass parent styles explicitly.
        return arrangeWithParent(region, children, parentStyles = Styles.NULL)
    }

    /**
     * Arrange with explicit parent styles. Containers should call this so the
     * grid metadata (`gridSize`, `gridColumns`, `gridRows`, `gridGutter`)
     * propagates correctly.
     */
    public fun arrangeWithParent(
        region: Region,
        children: List<Pair<Widget, Styles>>,
        parentStyles: Styles,
    ): List<Placement> {
        val visible = children.filter { (_, s) -> s.display != tools.konsole.textual.css.Display.None }
        if (visible.isEmpty()) return emptyList()

        val gridSize = parentStyles.gridSize ?: (visible.size to 1)
        val cols = gridSize.first.coerceAtLeast(1)
        val declaredRows = gridSize.second.coerceAtLeast(1)
        val (gutterH, gutterV) = parentStyles.gridGutter ?: (0 to 0)

        // ---- Pass 1: assign cells ---------------------------------------
        // occupancy[r][c] = true if a child already covers that cell.
        // Grow rows on demand so children beyond the declared grid extend down.
        val occupancy: MutableList<BooleanArray> = mutableListOf()
        fun ensureRow(r: Int) {
            while (r >= occupancy.size) occupancy += BooleanArray(cols)
        }

        data class CellSpan(val row: Int, val col: Int, val rowSpan: Int, val colSpan: Int)

        val assignments = mutableListOf<CellSpan>()
        var cursorRow = 0
        var cursorCol = 0
        for ((_, styles) in visible) {
            val rowSpan = (styles.rowSpan ?: 1).coerceIn(1, declaredRows.coerceAtLeast(1))
            val colSpan = (styles.columnSpan ?: 1).coerceIn(1, cols)
            // Find the next slot that fits the span, marching row-major.
            var placed = false
            while (!placed) {
                ensureRow(cursorRow)
                if (cursorCol + colSpan > cols) {
                    cursorCol = 0
                    cursorRow += 1
                    continue
                }
                ensureRow(cursorRow + rowSpan - 1)
                val fits = (0 until rowSpan).all { dr ->
                    (0 until colSpan).all { dc -> !occupancy[cursorRow + dr][cursorCol + dc] }
                }
                if (fits) {
                    for (dr in 0 until rowSpan) {
                        for (dc in 0 until colSpan) {
                            occupancy[cursorRow + dr][cursorCol + dc] = true
                        }
                    }
                    assignments += CellSpan(cursorRow, cursorCol, rowSpan, colSpan)
                    cursorCol += colSpan
                    placed = true
                } else {
                    cursorCol += 1
                }
            }
        }
        val totalRows = occupancy.size.coerceAtLeast(declaredRows)

        // ---- Pass 2: resolve track sizes --------------------------------
        val columnSizes = resolveTrackSizes(
            count = cols,
            available = (region.width - gutterH * (cols - 1)).coerceAtLeast(0),
            declared = parentStyles.gridColumns,
        )
        val rowSizes = resolveTrackSizes(
            count = totalRows,
            available = (region.height - gutterV * (totalRows - 1)).coerceAtLeast(0),
            declared = parentStyles.gridRows,
        )

        // Prefix offsets for each column / row (including gutters).
        val columnOffsets = IntArray(cols + 1).also {
            it[0] = 0
            for (i in 0 until cols) it[i + 1] = it[i] + columnSizes[i] + (if (i < cols - 1) gutterH else 0)
        }
        val rowOffsets = IntArray(totalRows + 1).also {
            it[0] = 0
            for (i in 0 until totalRows) it[i + 1] = it[i] + rowSizes[i] + (if (i < totalRows - 1) gutterV else 0)
        }

        // ---- Emit placements --------------------------------------------
        val placements = mutableListOf<Placement>()
        for ((i, cell) in assignments.withIndex()) {
            val (widget, _) = visible[i]
            val x = region.x + columnOffsets[cell.col]
            val y = region.y + rowOffsets[cell.row]
            // Span ends just before the gutter that would follow the last cell.
            val endX = region.x + columnOffsets[(cell.col + cell.colSpan).coerceAtMost(cols)] -
                if (cell.col + cell.colSpan < cols) gutterH else 0
            val endY = region.y + rowOffsets[(cell.row + cell.rowSpan).coerceAtMost(totalRows)] -
                if (cell.row + cell.rowSpan < totalRows) gutterV else 0
            val w = (endX - x).coerceAtLeast(0)
            val h = (endY - y).coerceAtLeast(0)
            placements += Placement(widget, Region(x, y, w, h))
        }
        return placements
    }

    /**
     * Compute the absolute pixel sizes of [count] tracks given a [declared]
     * list of [Scalar]s (cells / `%` / `fr` / `auto`) and [available] space
     * to share. When [declared] is shorter than [count], the last entry
     * repeats. When `null`, every track is `1fr`.
     */
    private fun resolveTrackSizes(count: Int, available: Int, declared: List<Scalar>?): IntArray {
        val tracks = IntArray(count)
        val list: List<Scalar> = if (declared.isNullOrEmpty()) {
            List(count) { Scalar(1.0, LengthUnit.Fraction) }
        } else {
            List(count) { i -> declared.getOrElse(i) { declared.last() } }
        }
        // First fix every non-fraction track. `auto` is treated as 1fr for now
        // (real auto sizing would measure children, deferred for later).
        var consumed = 0
        var totalFractions = 0.0
        for (i in 0 until count) {
            val s = list[i]
            tracks[i] = when {
                s.isFraction || s.isAuto -> 0
                else -> s.resolve(available).coerceAtLeast(0).also { consumed += it }
            }
            if (s.isFraction || s.isAuto) totalFractions += (if (s.isAuto) 1.0 else s.value)
        }
        // Distribute the remainder across fraction/auto tracks.
        val remaining = (available - consumed).coerceAtLeast(0)
        if (totalFractions > 0.0) {
            for (i in 0 until count) {
                val s = list[i]
                if (s.isFraction || s.isAuto) {
                    val share = if (s.isAuto) 1.0 else s.value
                    tracks[i] = ((share / totalFractions) * remaining).toInt().coerceAtLeast(0)
                }
            }
        }
        return tracks
    }
}
