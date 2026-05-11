package tools.konsole.textual.compositor

import tools.konsole.rich.Strip
import tools.konsole.rich.geometry.Region
import tools.konsole.textual.widget.Widget

/**
 * Minimal frame-renderer: maps widgets to regions and produces a list of [Strip]s
 * for the visible terminal. Mirrors Python textual's `Compositor` in concept; the
 * production version (Phase 8) layers in dirty tracking and per-cell diffs.
 *
 * Phase 7 deliverable: render a single screen's worth of widgets sequentially in
 * a vertical layout, with each widget receiving a [Region] that spans the full
 * terminal width and consumes consecutive rows. Containers / CSS grid / dock layouts
 * arrive in Phase 8.
 */
public class Compositor(public val viewport: Region) {

    /** Map of widget → region; populated by [arrange]. */
    public val placements: MutableList<Placement> = mutableListOf()

    public data class Placement(val widget: Widget, val region: Region, val zIndex: Int = 0)

    /** Vertical stack layout — give every visible widget the full width and one row. */
    public fun arrange(widgets: List<Widget>) {
        placements.clear()
        var y = viewport.y
        for (w in widgets) {
            val region = Region(viewport.x, y, viewport.width, 1)
            placements += Placement(w, region)
            y += 1
            if (y >= viewport.bottom) break
        }
    }

    /** Render every placement to a list of [Strip]s, one per row of the [viewport]. */
    public fun render(): List<Strip> {
        val lines = MutableList(viewport.height) { Strip.EMPTY }
        for (p in placements) {
            val relY = p.region.y - viewport.y
            if (relY !in 0 until lines.size) continue
            val strip = p.widget.renderLine(0, p.region.width)
            lines[relY] = strip.adjustCellLength(viewport.width)
        }
        return lines
    }
}
