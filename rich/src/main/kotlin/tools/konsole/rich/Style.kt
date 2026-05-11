package tools.konsole.rich

import tools.konsole.core.ColorSystem

import tools.konsole.core.style.Attribute
import tools.konsole.core.style.Attributes
import tools.konsole.core.style.Color
import tools.konsole.core.style.ContentStyle

/**
 * A rich-style. All fields are nullable so unset = inherit from parent.
 *
 * To collapse to konsole's flat [ContentStyle] for emission, call [toContentStyle].
 * The boolean fields are tri-valued (true = on, false = explicitly off, null = inherit) so
 * that markup like `[not bold]` can be represented mid-stream — konsole's [Attributes]
 * bitset cannot distinguish "off" from "inherit".
 */
public data class Style(
    public val color: Color? = null,
    public val bgcolor: Color? = null,
    public val underlineColor: Color? = null,
    public val bold: Boolean? = null,
    public val italic: Boolean? = null,
    public val underline: Boolean? = null,
    public val dim: Boolean? = null,
    public val reverse: Boolean? = null,
    public val strike: Boolean? = null,
    public val blink: Boolean? = null,
    public val blink2: Boolean? = null,
    public val conceal: Boolean? = null,
    public val frame: Boolean? = null,
    public val encircle: Boolean? = null,
    public val overline: Boolean? = null,
    public val link: String? = null,
    public val meta: Map<String, Any?> = emptyMap(),
) {
    /**
     * Combine two styles. Right-side non-null fields win; meta maps are merged.
     */
    public operator fun plus(other: Style): Style = Style(
        color = other.color ?: color,
        bgcolor = other.bgcolor ?: bgcolor,
        underlineColor = other.underlineColor ?: underlineColor,
        bold = other.bold ?: bold,
        italic = other.italic ?: italic,
        underline = other.underline ?: underline,
        dim = other.dim ?: dim,
        reverse = other.reverse ?: reverse,
        strike = other.strike ?: strike,
        blink = other.blink ?: blink,
        blink2 = other.blink2 ?: blink2,
        conceal = other.conceal ?: conceal,
        frame = other.frame ?: frame,
        encircle = other.encircle ?: encircle,
        overline = other.overline ?: overline,
        link = other.link ?: link,
        meta = if (other.meta.isEmpty()) meta else meta + other.meta,
    )

    /** True if every field is unset (= NULL). */
    public val isNull: Boolean get() = this == NULL

    /** Produce a konsole [ContentStyle] for emission, applying color downgrade. */
    public fun toContentStyle(colorSystem: ColorSystem): ContentStyle {
        val fg = color?.let { colorSystem.downgrade(it) }
        val bg = bgcolor?.let { colorSystem.downgrade(it) }
        val ul = underlineColor?.let { colorSystem.downgrade(it) }
        var attrs = Attributes.NONE
        if (bold == true) attrs += Attribute.Bold
        if (italic == true) attrs += Attribute.Italic
        if (underline == true) attrs += Attribute.Underlined
        if (dim == true) attrs += Attribute.Dim
        if (reverse == true) attrs += Attribute.Reverse
        if (strike == true) attrs += Attribute.CrossedOut
        if (blink == true) attrs += Attribute.SlowBlink
        if (blink2 == true) attrs += Attribute.RapidBlink
        if (conceal == true) attrs += Attribute.Hidden
        if (frame == true) attrs += Attribute.Framed
        if (encircle == true) attrs += Attribute.Encircled
        if (overline == true) attrs += Attribute.OverLined
        return ContentStyle(foreground = fg, background = bg, underline = ul, attributes = attrs)
    }

    public companion object {
        public val NULL: Style = Style()

        /**
         * Parse a rich-style string like "bold red on white", "link=https://...", "not bold".
         * Tokens (separated by whitespace):
         *   - bold/italic/underline/dim/reverse/strike/blink/blink2/conceal/frame/encircle/overline → set true
         *   - not <attr> → set false
         *   - on <color> → bgcolor
         *   - link=<url> → link
         *   - any other token → color (if not yet set), else throws
         * Color tokens accept everything konsole's [Color.parse] accepts: named, hex, rgb(...), ansi(N).
         */
        public fun parse(s: String): Style = StyleParser.parse(s)
    }
}
