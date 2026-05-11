package tools.konsole.textual.compositor

import tools.konsole.core.Ansi
import tools.konsole.core.style.Color
import tools.konsole.rich.Strip
import tools.konsole.rich.Style

/**
 * Convert composited [Strip]s into a single ANSI-encoded byte stream ready
 * for [tools.konsole.textual.driver.Driver.write]. Mirrors the rendering
 * tail of textual's compositor where Strips become bytes.
 *
 * Phase 9.9 implementation: per-segment SGR emission, with a `CSI 0 m`
 * reset between styled runs. Optimisation (deduplicating identical
 * adjacent styles, dropping no-op resets) is left for the dirty-region
 * rewrite in Phase 9.10.
 */
public object StripSerializer {

    /**
     * Serialise [strips] starting at the given screen coordinates.
     *
     * @param strips rows of [tools.konsole.rich.Segment]s to emit. Row N is
     *   written to the row `originY + N`, starting at column `originX + 0`.
     * @param originX column to position the cursor before each row.
     * @param originY row to position the cursor at for the first strip.
     * @param clearFirst whether to emit a screen-clear before drawing.
     */
    public fun serialize(
        strips: List<Strip>,
        originX: Int = 0,
        originY: Int = 0,
        clearFirst: Boolean = false,
    ): String {
        val sb = StringBuilder()
        if (clearFirst) {
            sb.append(Ansi.CSI).append("2J")
            sb.append(Ansi.CSI).append("H")
        }
        for ((i, strip) in strips.withIndex()) {
            sb.append(Ansi.CSI).append(originY + i + 1).append(';').append(originX + 1).append('H')
            writeStrip(sb, strip)
        }
        sb.append(Ansi.CSI).append("0m")
        return sb.toString()
    }

    /**
     * Diff [old] vs [new] row-by-row and emit only rows that differ.
     * For each changed row, position the cursor at the start of the row
     * and write the new strip. Same-row rendering means same SGR runs,
     * so consecutive identical strips are skipped entirely — typical
     * idle frames send zero bytes.
     */
    public fun serializeDiff(
        old: List<Strip>,
        new: List<Strip>,
        originX: Int = 0,
        originY: Int = 0,
    ): String {
        val sb = StringBuilder()
        val rows = minOf(old.size, new.size)
        var wroteAny = false
        for (i in 0 until rows) {
            if (stripsEqual(old[i], new[i])) continue
            sb.append(Ansi.CSI).append(originY + i + 1).append(';').append(originX + 1).append('H')
            writeStrip(sb, new[i])
            wroteAny = true
        }
        // Rows added beyond `old.size` (terminal grew) — emit them fresh.
        for (i in rows until new.size) {
            sb.append(Ansi.CSI).append(originY + i + 1).append(';').append(originX + 1).append('H')
            writeStrip(sb, new[i])
            wroteAny = true
        }
        if (wroteAny) sb.append(Ansi.CSI).append("0m")
        return sb.toString()
    }

    private fun stripsEqual(a: Strip, b: Strip): Boolean {
        if (a.cellLength != b.cellLength) return false
        if (a.segments.size != b.segments.size) return false
        for (i in a.segments.indices) {
            val sa = a.segments[i]
            val sb = b.segments[i]
            if (sa.text != sb.text) return false
            if (sa.style != sb.style) return false
        }
        return true
    }

    /** Render a single [strip] inline at the current cursor position. */
    public fun serializeInline(strip: Strip): String {
        val sb = StringBuilder()
        writeStrip(sb, strip)
        sb.append(Ansi.CSI).append("0m")
        return sb.toString()
    }

    private fun writeStrip(sb: StringBuilder, strip: Strip) {
        var lastStyle: Style? = null
        for (seg in strip.segments) {
            val newStyle = seg.style
            if (newStyle != lastStyle) {
                sb.append(Ansi.CSI).append("0m")
                if (newStyle != null && !newStyle.isNull) emitSgr(sb, newStyle)
                lastStyle = newStyle
            }
            sb.append(seg.text)
        }
    }

    private fun emitSgr(sb: StringBuilder, style: Style) {
        val params = mutableListOf<String>()
        if (style.bold == true) params += "1"
        if (style.dim == true) params += "2"
        if (style.italic == true) params += "3"
        if (style.underline == true) params += "4"
        if (style.blink == true) params += "5"
        if (style.reverse == true) params += "7"
        if (style.conceal == true) params += "8"
        if (style.strike == true) params += "9"
        style.color?.let { params += colorSgr(it, foreground = true) }
        style.bgcolor?.let { params += colorSgr(it, foreground = false) }
        if (params.isEmpty()) return
        sb.append(Ansi.CSI).append(params.joinToString(";")).append('m')
    }

    private fun colorSgr(color: Color, foreground: Boolean): String {
        val role = if (foreground) 38 else 48
        val direct = if (foreground) 30 else 40
        return when (color) {
            Color.Reset -> "${if (foreground) 39 else 49}"
            Color.Black -> "${direct + 0}"
            Color.DarkRed -> "${direct + 1}"
            Color.DarkGreen -> "${direct + 2}"
            Color.DarkYellow -> "${direct + 3}"
            Color.DarkBlue -> "${direct + 4}"
            Color.DarkMagenta -> "${direct + 5}"
            Color.DarkCyan -> "${direct + 6}"
            Color.Grey -> "${direct + 7}"
            Color.DarkGrey -> "${direct + 60}"   // bright black
            Color.Red -> "${direct + 61}"
            Color.Green -> "${direct + 62}"
            Color.Yellow -> "${direct + 63}"
            Color.Blue -> "${direct + 64}"
            Color.Magenta -> "${direct + 65}"
            Color.Cyan -> "${direct + 66}"
            Color.White -> "${direct + 67}"
            is Color.Rgb -> "$role;2;${color.r};${color.g};${color.b}"
            is Color.AnsiValue -> "$role;5;${color.value}"
        }
    }
}
