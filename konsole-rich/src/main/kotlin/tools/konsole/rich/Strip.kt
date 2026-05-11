package tools.konsole.rich

/**
 * One rendered line: an immutable list of [Segment]s plus a cached cell length.
 * Promoted from textual into the rich foundation because [Segment]-stream renderers
 * (Table, Live, LiveRender, Compositor) all want O(1) length access.
 *
 * A `Strip` is *not* aware of styles beyond what's in its segments — it is a
 * carrier for already-styled output. Operations that need style-aware behavior
 * (joining strips of different background colors, etc.) live on the renderer.
 */
public class Strip private constructor(
    public val segments: List<Segment>,
    private val cachedCellLength: Int,
) : Iterable<Segment> {

    /** Total cell-width across all non-control segments. */
    public val cellLength: Int get() = cachedCellLength

    public val isEmpty: Boolean get() = segments.isEmpty() || cachedCellLength == 0

    override fun iterator(): Iterator<Segment> = segments.iterator()

    /** Concatenate two strips. */
    public operator fun plus(other: Strip): Strip = Strip(
        segments = segments + other.segments,
        cachedCellLength = cachedCellLength + other.cachedCellLength,
    )

    /**
     * Pad or truncate the strip to exactly [width] cells.
     *
     * @param width target cell width (≥ 0)
     * @param style style applied to padding cells (null = default/unset)
     */
    public fun adjustCellLength(width: Int, style: Style? = null): Strip {
        if (width == cachedCellLength) return this
        if (width < 0) error("width must be non-negative, was $width")
        if (cachedCellLength < width) {
            val padding = " ".repeat(width - cachedCellLength)
            return Strip(segments + Segment(padding, style), width)
        }
        // Truncate at the cell boundary
        val out = ArrayList<Segment>(segments.size)
        var remaining = width
        for (seg in segments) {
            if (seg.control != null) { out += seg; continue }
            val segLen = Cells.cellLen(seg.text)
            if (segLen <= remaining) {
                out += seg
                remaining -= segLen
                if (remaining == 0) break
            } else {
                out += Segment(Cells.setCellSize(seg.text, remaining), seg.style, seg.control)
                remaining = 0
                break
            }
        }
        return Strip(out, width)
    }

    public companion object {
        public val EMPTY: Strip = Strip(emptyList(), 0)

        public fun of(vararg segments: Segment): Strip = of(segments.toList())

        public fun of(segments: List<Segment>): Strip {
            if (segments.isEmpty()) return EMPTY
            val len = segments.sumOf { if (it.control == null) Cells.cellLen(it.text) else 0 }
            return Strip(segments.toList(), len)
        }
    }
}
