package tools.konsole.rich

/**
 * Force [renderable] to render within at most [width] cells. Mirrors rich's
 * `constrain.Constrain` — used when a parent wants to cap the inner element's
 * effective width independent of the surrounding context.
 *
 * If [width] is null, this is a transparent passthrough.
 */
public data class Constrain(
    public val renderable: Renderable,
    public val width: Int? = null,
) : Renderable, Measurable {

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> {
        val effective = if (width != null && width < options.maxWidth) {
            options.copy(maxWidth = width)
        } else {
            options
        }
        return renderable.render(console, effective)
    }

    override fun measure(console: Console, options: RenderOptions): Measurement {
        val inner = if (renderable is Measurable) {
            renderable.measure(console, options)
        } else {
            Measurement(0, options.maxWidth)
        }
        if (width == null) return inner
        return Measurement(
            minimum = inner.minimum.coerceAtMost(width),
            maximum = inner.maximum.coerceAtMost(width),
        )
    }
}
