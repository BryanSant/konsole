package tools.konsole.rich.export

import tools.konsole.core.ColorSystem
import tools.konsole.rich.Console
import tools.konsole.rich.Control
import tools.konsole.rich.Segment
import tools.konsole.rich.Style
import tools.konsole.core.style.Color

/**
 * Export the captured [Segment] buffer (set [Console.record] = true) to plain text, HTML, or SVG.
 *
 * Mirrors `Console.export_text` / `export_html` / `export_svg`.
 */
public object Export {

    public fun text(segments: List<Segment>): String {
        val sb = StringBuilder()
        for (seg in segments) {
            when {
                seg.control is Control.NewLine -> sb.append('\n')
                seg.control != null -> { /* skip non-text control segments */ }
                else -> sb.append(seg.text)
            }
        }
        return sb.toString()
    }

    public fun html(
        segments: List<Segment>,
        title: String = "konsole-rich",
        codeStyle: String = "background-color: #1e1e1e; color: #f8f8f2; padding: 12px; border-radius: 6px; font-family: 'JetBrains Mono', 'Fira Code', monospace; font-size: 14px;",
    ): String {
        val sb = StringBuilder()
        sb.append("<!doctype html>\n<html><head><meta charset=\"utf-8\"><title>")
        sb.append(escape(title))
        sb.append("</title></head><body><pre style=\"")
        sb.append(escape(codeStyle))
        sb.append("\">")
        for (seg in segments) {
            when {
                seg.control is Control.NewLine -> sb.append('\n')
                seg.control is Control.Link -> sb.append("<a href=\"").append(escape(seg.control.uri)).append("\">")
                seg.control is Control.LinkEnd -> sb.append("</a>")
                seg.control != null -> { /* skip */ }
                else -> {
                    val css = cssFor(seg.style)
                    if (css == null) {
                        sb.append(escape(seg.text))
                    } else {
                        sb.append("<span style=\"").append(css).append("\">")
                        sb.append(escape(seg.text))
                        sb.append("</span>")
                    }
                }
            }
        }
        sb.append("</pre></body></html>\n")
        return sb.toString()
    }

    public fun svg(
        segments: List<Segment>,
        title: String = "konsole-rich",
        cellWidth: Int = 8,
        cellHeight: Int = 17,
        padding: Int = 16,
        background: String = "#1e1e1e",
        defaultColor: String = "#f8f8f2",
        fontFamily: String = "'JetBrains Mono', 'Fira Code', monospace",
        fontSize: Int = 14,
    ): String {
        // Lay out into per-line cells.
        val lines: MutableList<MutableList<Segment>> = mutableListOf(mutableListOf())
        for (seg in segments) {
            if (seg.control is Control.NewLine) lines.add(mutableListOf())
            else if (seg.control == null && seg.text.isNotEmpty()) lines.last().add(seg)
        }
        val cols = lines.maxOfOrNull { it.sumOf { s -> s.text.length } } ?: 0
        val width = cols * cellWidth + padding * 2
        val height = lines.size * cellHeight + padding * 2

        val sb = StringBuilder()
        sb.append("""<svg xmlns="http://www.w3.org/2000/svg" width="$width" height="$height" font-family="${escape(fontFamily)}" font-size="$fontSize">""")
        sb.append("\n<rect width=\"100%\" height=\"100%\" fill=\"").append(background).append("\"/>\n")
        sb.append("<title>").append(escape(title)).append("</title>\n")
        for ((row, line) in lines.withIndex()) {
            val y = padding + (row + 1) * cellHeight - 4
            var x = padding
            for (seg in line) {
                val color = seg.style?.color?.let { hexOf(it) } ?: defaultColor
                val weight = if (seg.style?.bold == true) "bold" else "normal"
                val styleAttr = if (seg.style?.italic == true) " font-style=\"italic\"" else ""
                sb.append("<text x=\"$x\" y=\"$y\" fill=\"$color\" font-weight=\"$weight\"$styleAttr>")
                sb.append(escape(seg.text))
                sb.append("</text>\n")
                x += seg.text.length * cellWidth
            }
        }
        sb.append("</svg>\n")
        return sb.toString()
    }

    private fun cssFor(style: Style?): String? {
        if (style == null || style.isNull) return null
        val parts = mutableListOf<String>()
        style.color?.let { parts += "color:${hexOf(it)}" }
        style.bgcolor?.let { parts += "background-color:${hexOf(it)}" }
        if (style.bold == true) parts += "font-weight:bold"
        if (style.italic == true) parts += "font-style:italic"
        if (style.underline == true) parts += "text-decoration:underline"
        if (style.strike == true) parts += "text-decoration:line-through"
        if (style.dim == true) parts += "opacity:0.7"
        return if (parts.isEmpty()) null else parts.joinToString(";")
    }

    private fun hexOf(color: Color): String = when (color) {
        Color.Reset -> "inherit"
        Color.Black -> "#000000"
        Color.DarkRed -> "#800000"
        Color.DarkGreen -> "#008000"
        Color.DarkYellow -> "#808000"
        Color.DarkBlue -> "#000080"
        Color.DarkMagenta -> "#800080"
        Color.DarkCyan -> "#008080"
        Color.Grey -> "#c0c0c0"
        Color.DarkGrey -> "#808080"
        Color.Red -> "#ff0000"
        Color.Green -> "#00ff00"
        Color.Yellow -> "#ffff00"
        Color.Blue -> "#0000ff"
        Color.Magenta -> "#ff00ff"
        Color.Cyan -> "#00ffff"
        Color.White -> "#ffffff"
        is Color.Rgb -> "#%02x%02x%02x".format(color.r, color.g, color.b)
        is Color.AnsiValue -> "#" + ansiHex(color.value)
    }

    private fun ansiHex(idx: Int): String {
        // Reuse ColorDowngrade.ansi256Rgb logic inline to avoid an internal import.
        val (r, g, b) = when {
            idx < 16 -> when (idx) {
                0 -> Triple(0, 0, 0); 1 -> Triple(128, 0, 0); 2 -> Triple(0, 128, 0); 3 -> Triple(128, 128, 0)
                4 -> Triple(0, 0, 128); 5 -> Triple(128, 0, 128); 6 -> Triple(0, 128, 128); 7 -> Triple(192, 192, 192)
                8 -> Triple(128, 128, 128); 9 -> Triple(255, 0, 0); 10 -> Triple(0, 255, 0); 11 -> Triple(255, 255, 0)
                12 -> Triple(0, 0, 255); 13 -> Triple(255, 0, 255); 14 -> Triple(0, 255, 255); else -> Triple(255, 255, 255)
            }
            idx in 16..231 -> {
                val n = idx - 16
                val levels = intArrayOf(0, 95, 135, 175, 215, 255)
                Triple(levels[(n / 36) % 6], levels[(n / 6) % 6], levels[n % 6])
            }
            else -> {
                val v = 8 + (idx - 232) * 10
                Triple(v, v, v)
            }
        }
        return "%02x%02x%02x".format(r, g, b)
    }

    private fun escape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
