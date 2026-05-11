package tools.konsole.textual.compositor

import tools.konsole.rich.Segment
import tools.konsole.rich.Strip
import tools.konsole.rich.geometry.Region
import tools.konsole.textual.widget.Widget

/**
 * Layered frame renderer. Mirrors Python textual's `Compositor` —
 * widgets are placed into named [Layer]s, composited back-to-front so
 * higher z-order overlays cover lower ones, and rendered into a list of
 * [Strip]s sized to the [viewport].
 *
 * Hit-testing returns the topmost widget at a screen coordinate, used by
 * mouse routing.
 *
 * Anchored overlays (Toast/Tooltip/Select dropdown) attach to a parent
 * widget's region; helpers [placeBelow]/[placeAbove]/[placeRightOf] compute
 * the overlay's screen position from the parent's [Region] and clamp to
 * the viewport so popups never escape the screen.
 *
 * Phase 9.8 deliverables:
 *   - z-ordered layered placement
 *   - anchored overlay helpers
 *   - back-to-front compositing
 *   - hit-testing
 *
 * Deferred to Phase 9.9+ (when the full incremental render pipeline lands):
 *   - dirty-region tracking (per-cell diff vs. full-frame redraw)
 *   - widget Z-index within a layer (currently same-layer placements render
 *     in insertion order)
 */
public class Compositor(initialViewport: Region) {

    /** Current viewport. Use [resize] to change it. */
    public var viewport: Region = initialViewport
        private set

    /**
     * Resize the rendering viewport. Existing placements are preserved but
     * may now sit outside the new bounds; callers typically follow up with
     * [clear] + a fresh `arrange()` from their App's render loop.
     */
    public fun resize(newViewport: Region) {
        viewport = newViewport
    }

    /** A named depth layer. Higher [zOrder] renders on top of lower. */
    public data class Layer(public val name: String, public val zOrder: Int) : Comparable<Layer> {
        override fun compareTo(other: Layer): Int = zOrder.compareTo(other.zOrder)
    }

    public companion object {
        /** Default layer — regular widget content. */
        public val BASE: Layer = Layer("base", 0)
        /** Generic overlays (Select dropdowns, popovers). */
        public val OVERLAY: Layer = Layer("overlay", 10)
        /** Toast notifications. */
        public val TOAST: Layer = Layer("toast", 20)
        /** Modal popups / dialogs. */
        public val POPUP: Layer = Layer("popup", 30)
        /** Tooltips — render above everything except modals if you push them higher. */
        public val TOOLTIP: Layer = Layer("tooltip", 40)
    }

    /** A widget placement at a specific region on a specific layer. */
    public data class Placement(public val widget: Widget, public val region: Region, public val layer: Layer)

    private val _placements: MutableList<Placement> = mutableListOf()
    public val placements: List<Placement> get() = _placements.toList()

    /**
     * Place [widget] explicitly at [region] on [layer]. If [widget] is a
     * [tools.konsole.textual.widgets.Container], its children are recursively
     * arranged inside [region] using the matching
     * [tools.konsole.textual.layouts.Layout]. The container itself is still
     * registered so hit-testing / overlay anchoring against it work, but its
     * own [Widget.render] is expected to be transparent (Containers ship with
     * an empty default [Widget.render]).
     */
    public fun placeAt(widget: Widget, region: Region, layer: Layer = BASE) {
        _placements += Placement(widget, region, layer)
        if (widget is tools.konsole.textual.widgets.Container) {
            recursivelyPlaceContainerChildren(widget, region, layer)
        }
    }

    private fun recursivelyPlaceContainerChildren(
        container: tools.konsole.textual.widgets.Container,
        region: Region,
        layer: Layer,
    ) {
        val children = container.containerChildren
        if (children.isEmpty()) return
        val pairs = children.map { it to container.childStyles(it) }
        val layout = tools.konsole.textual.layouts.Layout.forKind(container.layout)
        val placements = if (container.layout == tools.konsole.textual.css.LayoutKind.Grid) {
            tools.konsole.textual.layouts.GridLayout
                .arrangeWithParent(region, pairs, container.containerStyles)
        } else {
            layout.arrange(region, pairs)
        }
        for (p in placements) {
            placeAt(p.widget, p.region, layer)  // recurses into nested containers
        }
    }

    /**
     * Vertical-stack layout: give each widget the full viewport width and
     * one row of height. Preserves the Phase 7 behaviour for existing call
     * sites that don't care about layering. Each widget is routed through
     * [placeAt] so [tools.konsole.textual.widgets.Container] children
     * recurse into their declared layout.
     */
    public fun arrange(widgets: List<Widget>, layer: Layer = BASE) {
        _placements.clear()
        var y = viewport.y
        for (w in widgets) {
            placeAt(w, Region(viewport.x, y, viewport.width, 1), layer)
            y += 1
            if (y >= viewport.bottom) break
        }
    }

    /**
     * Place [overlay] below [anchor] (right under its bottom edge), aligned
     * to the anchor's left edge, [width] cells wide and [height] cells tall.
     * If there isn't enough room below the anchor, the overlay flips above
     * (mirroring textual's overflow behaviour).
     *
     * Returns the computed [Region] so callers can adjust further.
     */
    public fun placeBelow(
        overlay: Widget,
        anchor: Region,
        width: Int = anchor.width,
        height: Int = 5,
        layer: Layer = OVERLAY,
    ): Region {
        val region = anchorRegionBelow(anchor, width, height)
        _placements += Placement(overlay, region, layer)
        return region
    }

    public fun placeAbove(
        overlay: Widget,
        anchor: Region,
        width: Int = anchor.width,
        height: Int = 5,
        layer: Layer = OVERLAY,
    ): Region {
        val region = anchorRegionAbove(anchor, width, height)
        _placements += Placement(overlay, region, layer)
        return region
    }

    public fun placeRightOf(
        overlay: Widget,
        anchor: Region,
        width: Int = 20,
        height: Int = anchor.height,
        layer: Layer = OVERLAY,
    ): Region {
        val region = anchorRegionRightOf(anchor, width, height)
        _placements += Placement(overlay, region, layer)
        return region
    }

    /** Remove all placements. */
    public fun clear() {
        _placements.clear()
    }

    /** Remove every placement on [layer]. */
    public fun clearLayer(layer: Layer) {
        _placements.removeAll { it.layer == layer }
    }

    /** Remove the first placement of [widget] (across any layer). */
    public fun removePlacement(widget: Widget): Boolean =
        _placements.removeAll { it.widget === widget }

    /**
     * Hit-test: return the topmost widget at the screen coordinate `(x, y)`,
     * or `null` if no placement covers that cell. "Topmost" = highest layer
     * z-order, breaking ties by last-placed-wins.
     */
    public fun hitTest(x: Int, y: Int): Widget? {
        var best: Placement? = null
        for (p in _placements) {
            if (x in p.region.x until p.region.right && y in p.region.y until p.region.bottom) {
                if (best == null || p.layer >= best.layer) best = p
            }
        }
        return best?.widget
    }

    /** Render every layer back-to-front, returning the viewport's row strips. */
    public fun render(): List<Strip> {
        val rows = MutableList(viewport.height) { mutableListOf<Cell>() }
        for (rowIdx in 0 until viewport.height) {
            for (col in 0 until viewport.width) rows[rowIdx].add(Cell.EMPTY)
        }
        // Sort placements by layer z-order so higher layers overwrite lower.
        val sorted = _placements.sortedBy { it.layer.zOrder }
        for (p in sorted) {
            renderInto(p, rows)
        }
        return rows.map { row ->
            val segments = mutableListOf<Segment>()
            for (cell in row) segments += Segment(cell.ch.toString(), cell.style)
            Strip.of(segments).adjustCellLength(viewport.width)
        }
    }

    private fun renderInto(p: Placement, rows: MutableList<MutableList<Cell>>) {
        // Expose the placement region to the widget so its render() can size
        // content to the actual available width/height.
        p.widget.lastRegion = p.region
        val widgetWidth = p.region.width.coerceAtLeast(0)
        if (widgetWidth == 0) return
        // For Scrollable widgets, the visible window slides over a larger
        // content area: line `localY` of the region maps to content line
        // `localY + scrollY` on the widget. Non-scrollable widgets see
        // localY directly.
        val scrollable = p.widget as? tools.konsole.textual.widget.Scrollable
        val scrollY = scrollable?.scrollY ?: 0
        val scrollX = scrollable?.scrollX ?: 0
        // Render the line at the scrolled-into-content position. For
        // horizontal scrolling we ask the widget for a wider strip
        // (widgetWidth + scrollX) and then drop the leading scrollX cells.
        val requestedWidth = if (scrollX > 0) widgetWidth + scrollX else widgetWidth
        for (localY in 0 until p.region.height) {
            val absY = p.region.y + localY - viewport.y
            if (absY !in rows.indices) continue
            val contentY = localY + scrollY
            val strip = try { p.widget.renderLine(contentY, requestedWidth) } catch (_: Throwable) { Strip.EMPTY }
            val sized = strip.adjustCellLength(requestedWidth)
            var col = 0
            for (seg in sized) {
                for (ch in seg.text) {
                    if (col >= scrollX) {
                        val outCol = col - scrollX
                        if (outCol >= widgetWidth) break
                        val absX = p.region.x + outCol - viewport.x
                        if (absX in 0 until viewport.width) {
                            rows[absY][absX] = Cell(ch, seg.style)
                        }
                    }
                    col += 1
                    if (col >= requestedWidth) break
                }
                if (col >= requestedWidth) break
            }
        }
    }

    private fun anchorRegionBelow(anchor: Region, width: Int, height: Int): Region {
        val x = anchor.x.coerceAtLeast(viewport.x)
        val belowY = anchor.bottom
        val aboveY = anchor.y - height
        val fitsBelow = belowY + height <= viewport.bottom
        val y = if (fitsBelow) belowY else maxOf(viewport.y, aboveY)
        val clampedW = minOf(width, viewport.right - x)
        val clampedH = minOf(height, viewport.bottom - y)
        return Region(x, y, clampedW.coerceAtLeast(0), clampedH.coerceAtLeast(0))
    }

    private fun anchorRegionAbove(anchor: Region, width: Int, height: Int): Region {
        val x = anchor.x.coerceAtLeast(viewport.x)
        val aboveY = anchor.y - height
        val belowY = anchor.bottom
        val fitsAbove = aboveY >= viewport.y
        val y = if (fitsAbove) aboveY else minOf(viewport.bottom - height, belowY)
        val clampedW = minOf(width, viewport.right - x)
        val clampedH = minOf(height, viewport.bottom - y).coerceAtLeast(0)
        return Region(x, y, clampedW.coerceAtLeast(0), clampedH)
    }

    private fun anchorRegionRightOf(anchor: Region, width: Int, height: Int): Region {
        val rightX = anchor.right
        val leftX = anchor.x - width
        val fitsRight = rightX + width <= viewport.right
        val x = if (fitsRight) rightX else maxOf(viewport.x, leftX)
        val y = anchor.y.coerceAtLeast(viewport.y)
        val clampedW = minOf(width, viewport.right - x).coerceAtLeast(0)
        val clampedH = minOf(height, viewport.bottom - y).coerceAtLeast(0)
        return Region(x, y, clampedW, clampedH)
    }

    private data class Cell(val ch: Char, val style: tools.konsole.rich.Style?) {
        companion object { val EMPTY: Cell = Cell(' ', null) }
    }
}
