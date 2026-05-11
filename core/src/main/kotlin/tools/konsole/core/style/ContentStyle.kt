package tools.konsole.core.style

/**
 * A combined foreground / background / underline color and attribute bitset,
 * applied to content via [apply] or [StyledContent].
 */
public data class ContentStyle(
    val foreground: Color? = null,
    val background: Color? = null,
    val underline: Color? = null,
    val attributes: Attributes = Attributes.NONE,
) {
    /** Wrap [content] with this style. */
    public fun <D> apply(content: D): StyledContent<D> = StyledContent(this, content)

    public companion object {
        public val EMPTY: ContentStyle = ContentStyle()
    }
}
