package tools.konsole.rich

/**
 * A unit of rendered output: text + optional style + optional control marker.
 *
 * Segments stream through the rendering pipeline (resolve → wrap → emit) before
 * being converted to konsole [tools.konsole.core.Command]s. Their [text] is
 * never expected to contain `\n` except via [Control.NewLine].
 */
public data class Segment(
    public val text: String,
    public val style: Style? = null,
    public val control: Control? = null,
) {
    public val cellLength: Int get() = if (control != null && text.isEmpty()) 0 else text.length

    public companion object {
        /** End-of-line marker. The pipeline uses this to know when to emit `ResetColor + \n`. */
        public val LINE: Segment = Segment("\n", null, Control.NewLine)

        public fun text(text: String, style: Style? = null): Segment = Segment(text, style)
    }
}

/**
 * Out-of-band metadata attached to a [Segment]. The emitter inspects these to emit
 * special escape sequences (link open/close) or to feed exporters.
 */
public sealed interface Control {
    public data object NewLine : Control
    public data class Link(public val uri: String) : Control
    public data object LinkEnd : Control
    public data class Meta(public val data: Map<String, Any?>) : Control
}
