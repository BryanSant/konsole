package tools.konsole.rich

/**
 * Apply [style] as a base-layer overlay to [renderable]. Mirrors rich's
 * `styled.Styled` — every emitted [Segment]'s style is composed via
 * `style + segment.style` (right-side wins on overlap).
 *
 * Useful when a parent wants to colorise/dim a child without modifying the
 * child's own styling (e.g. dim the contents of a disabled panel).
 */
public data class Styled(
    public val renderable: Renderable,
    public val style: Style,
) : Renderable, Measurable {

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> = sequence {
        for (seg in renderable.render(console, options)) {
            if (seg.control != null) {
                yield(seg)
            } else {
                val merged = if (seg.style == null) style else style + seg.style
                yield(Segment(seg.text, merged, null))
            }
        }
    }

    override fun measure(console: Console, options: RenderOptions): Measurement =
        if (renderable is Measurable) renderable.measure(console, options)
        else Measurement(0, options.maxWidth)
}
