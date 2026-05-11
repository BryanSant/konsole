package tools.konsole.textual.layouts

import tools.konsole.rich.geometry.Region
import tools.konsole.rich.geometry.Spacing
import tools.konsole.textual.css.LayoutKind
import tools.konsole.textual.css.Scalar
import tools.konsole.textual.css.Styles
import tools.konsole.textual.css.Unit
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
 * Phase-8 placeholder for grid layout — falls back to [VerticalLayout].
 * Full grid (rows/columns/spans/gaps/fr units) lands in Phase 9 alongside
 * the widgets that depend on it (DataTable, etc.).
 */
public object GridLayout : Layout {
    override fun arrange(region: Region, children: List<Pair<Widget, Styles>>): List<Placement> =
        VerticalLayout.arrange(region, children)
}
