package tools.konsole.rich

import tools.konsole.rich.text.Justify
import tools.konsole.rich.text.Overflow

/**
 * Per-render constraints threaded down through nested [Renderable]s.
 *
 * Children may copy this with reduced [maxWidth] / [height] before recursing
 * (e.g. a [tools.konsole.rich.panel.Panel] subtracts borders + padding).
 */
public data class RenderOptions(
    public val maxWidth: Int,
    public val minWidth: Int = 1,
    public val justify: Justify = Justify.Default,
    public val overflow: Overflow = Overflow.Fold,
    public val noWrap: Boolean = false,
    public val height: Int? = null,
    public val highlight: Boolean = true,
    public val markup: Boolean = true,
) {
    public fun withMaxWidth(width: Int): RenderOptions =
        copy(maxWidth = width.coerceAtLeast(0))

    public fun update(
        maxWidth: Int? = null,
        justify: Justify? = null,
        overflow: Overflow? = null,
        noWrap: Boolean? = null,
        height: Int? = null,
    ): RenderOptions = copy(
        maxWidth = maxWidth ?: this.maxWidth,
        justify = justify ?: this.justify,
        overflow = overflow ?: this.overflow,
        noWrap = noWrap ?: this.noWrap,
        height = height ?: this.height,
    )
}
