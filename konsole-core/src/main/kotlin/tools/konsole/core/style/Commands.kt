package tools.konsole.core.style

import tools.konsole.core.Command
import tools.konsole.core.Ansi.CSI

/** Set the active foreground color. */
public data class SetForegroundColor(val color: Color) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(CSI)
        val sb = StringBuilder()
        color.sgrParams(role = 38, into = sb)
        out.append(sb).append('m')
    }
}

/** Set the active background color. */
public data class SetBackgroundColor(val color: Color) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(CSI)
        val sb = StringBuilder()
        color.sgrParams(role = 48, into = sb)
        out.append(sb).append('m')
    }
}

/** Set the underline color (terminals supporting CSI 58). */
public data class SetUnderlineColor(val color: Color) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(CSI)
        val sb = StringBuilder()
        color.sgrParams(role = 58, into = sb)
        out.append(sb).append('m')
    }
}

/** Set foreground and background atomically. */
public data class SetColors(val foreground: Color?, val background: Color?) : Command {
    override fun writeAnsi(out: Appendable) {
        if (foreground == null && background == null) return
        out.append(CSI)
        val sb = StringBuilder()
        var first = true
        foreground?.let { it.sgrParams(role = 38, into = sb); first = false }
        background?.let {
            if (!first) sb.append(';')
            it.sgrParams(role = 48, into = sb)
        }
        out.append(sb).append('m')
    }
}

/** Apply a single attribute. */
public data class SetAttribute(val attribute: Attribute) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(CSI).append(attribute.sgr.toString()).append('m')
    }
}

/** Apply a bitset of attributes. */
public data class SetAttributes(val attributes: Attributes) : Command {
    override fun writeAnsi(out: Appendable) {
        if (attributes.isEmpty()) return
        out.append(CSI)
        var first = true
        for (a in attributes) {
            if (!first) out.append(';')
            out.append(a.sgr.toString())
            first = false
        }
        out.append('m')
    }
}

/** Apply colors and attributes from a [ContentStyle] in one SGR. */
public data class SetStyle(val style: ContentStyle) : Command {
    override fun writeAnsi(out: Appendable) {
        if (style == ContentStyle.EMPTY) return
        out.append(CSI)
        val sb = StringBuilder()
        var first = true
        fun sep() { if (!first) sb.append(';'); first = false }

        style.foreground?.let { sep(); it.sgrParams(role = 38, into = sb) }
        style.background?.let { sep(); it.sgrParams(role = 48, into = sb) }
        style.underline?.let { sep(); it.sgrParams(role = 58, into = sb) }
        for (attr in style.attributes) { sep(); sb.append(attr.sgr) }
        out.append(sb).append('m')
    }
}

/** Print [content] using its style (a [StyledContent] already renders SGR + reset in toString). */
public data class PrintStyledContent<D>(val content: StyledContent<D>) : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(content.toString())
    }
}

/** Reset all colors and attributes (SGR 0). */
public data object ResetColor : Command {
    override fun writeAnsi(out: Appendable) {
        out.append(CSI).append("0m")
    }
}
