package tools.konsole.core.style

import tools.konsole.core.Ansi.CSI

/**
 * Display content (string, char, anything `toString`-able) tagged with a [ContentStyle].
 *
 * The [toString] result emits SGR-open + content + SGR-reset, so styled content
 * composes naturally:
 *
 * ```
 * println("hello".red().bold())
 * ```
 */
public data class StyledContent<out D>(public val style: ContentStyle, public val content: D) {

    override fun toString(): String = buildString {
        appendOpen(this)
        append(content.toString())
        appendClose(this)
    }

    private fun appendOpen(sb: StringBuilder) {
        if (style == ContentStyle.EMPTY) return
        sb.append(CSI)
        var first = true
        fun sep() { if (!first) sb.append(';'); first = false }

        style.foreground?.let { sep(); it.sgrParams(role = 38, into = sb) }
        style.background?.let { sep(); it.sgrParams(role = 48, into = sb) }
        style.underline?.let { sep(); it.sgrParams(role = 58, into = sb) }
        for (attr in style.attributes) {
            sep(); sb.append(attr.sgr)
        }
        sb.append('m')
    }

    private fun appendClose(sb: StringBuilder) {
        if (style == ContentStyle.EMPTY) return
        sb.append(CSI).append("0m")
    }
}
