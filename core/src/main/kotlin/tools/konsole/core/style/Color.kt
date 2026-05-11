package tools.konsole.core.style

/**
 * A terminal color, used for foreground, background, or underline.
 *
 * Crossterm parity:
 * - 16 named colors (`Black`, `Red`, …, `DarkRed`, …, `White`)
 * - [Reset] to clear an explicit color
 * - [Rgb] for 24-bit truecolor
 * - [AnsiValue] for the 256-color palette
 */
public sealed interface Color {

    /** Reset to the terminal's default color. */
    public data object Reset : Color

    public data object Black : Color
    public data object DarkRed : Color
    public data object DarkGreen : Color
    public data object DarkYellow : Color
    public data object DarkBlue : Color
    public data object DarkMagenta : Color
    public data object DarkCyan : Color
    public data object Grey : Color

    public data object DarkGrey : Color
    public data object Red : Color
    public data object Green : Color
    public data object Yellow : Color
    public data object Blue : Color
    public data object Magenta : Color
    public data object Cyan : Color
    public data object White : Color

    /** 24-bit truecolor. */
    public data class Rgb(val r: Int, val g: Int, val b: Int) : Color {
        init {
            require(r in 0..255 && g in 0..255 && b in 0..255) { "RGB components must be in 0..255: ($r, $g, $b)" }
        }
    }

    /** A color from the 256-color palette (0..255). */
    public data class AnsiValue(val value: Int) : Color {
        init {
            require(value in 0..255) { "AnsiValue must be in 0..255: $value" }
        }
    }

    public companion object {
        /**
         * Parse a color name or literal:
         * `red`, `dark_red`, `rgb(r,g,b)`, `ansi(n)`, `#rrggbb`, `#rgb`, etc.
         * Returns null for unknown names rather than throwing.
         */
        public fun parse(s: String): Color? {
            val name = s.trim().lowercase()
            return when (name) {
                "reset" -> Reset
                "black" -> Black
                "dark_red" -> DarkRed
                "dark_green" -> DarkGreen
                "dark_yellow" -> DarkYellow
                "dark_blue" -> DarkBlue
                "dark_magenta" -> DarkMagenta
                "dark_cyan" -> DarkCyan
                "grey", "gray" -> Grey
                "dark_grey", "dark_gray" -> DarkGrey
                "red" -> Red
                "green" -> Green
                "yellow" -> Yellow
                "blue" -> Blue
                "magenta" -> Magenta
                "cyan" -> Cyan
                "white" -> White
                else -> parseHex(name) ?: parseRgb(name) ?: parseAnsi(name)
            }
        }

        private val rgbPattern  = Regex("""rgb\((\d+),\s*(\d+),\s*(\d+)\)""")
        private val ansiPattern = Regex("""ansi\((\d+)\)""")
        private val hex6Pattern = Regex("""#([0-9a-f]{6})""")
        private val hex3Pattern = Regex("""#([0-9a-f]{3})""")

        private fun parseRgb(s: String): Rgb? = rgbPattern.matchEntire(s)?.let {
            val (r, g, b) = it.destructured
            try { Rgb(r.toInt(), g.toInt(), b.toInt()) } catch (_: IllegalArgumentException) { null }
        }

        private fun parseAnsi(s: String): AnsiValue? = ansiPattern.matchEntire(s)?.let {
            val v = it.groupValues[1].toIntOrNull() ?: return null
            try { AnsiValue(v) } catch (_: IllegalArgumentException) { null }
        }

        private fun parseHex(s: String): Rgb? {
            hex6Pattern.matchEntire(s)?.let { m ->
                val h = m.groupValues[1]
                return Rgb(h.substring(0, 2).toInt(16), h.substring(2, 4).toInt(16), h.substring(4, 6).toInt(16))
            }
            hex3Pattern.matchEntire(s)?.let { m ->
                val h = m.groupValues[1]
                // Expand each nibble: #rgb → #rrggbb  (digit * 0x11 = digit * 17)
                return Rgb(h[0].digitToInt(16) * 17, h[1].digitToInt(16) * 17, h[2].digitToInt(16) * 17)
            }
            return null
        }
    }
}

/**
 * Render this color as the SGR parameter list for the given role.
 * `role` is `38` for foreground, `48` for background, `58` for underline.
 */
internal fun Color.sgrParams(role: Int, into: StringBuilder) {
    when (this) {
        Color.Reset -> into.append(role + 1)              // 39 / 49 / 59 — default
        Color.Black -> into.append(role).append(";5;0")
        Color.DarkRed -> into.append(role).append(";5;1")
        Color.DarkGreen -> into.append(role).append(";5;2")
        Color.DarkYellow -> into.append(role).append(";5;3")
        Color.DarkBlue -> into.append(role).append(";5;4")
        Color.DarkMagenta -> into.append(role).append(";5;5")
        Color.DarkCyan -> into.append(role).append(";5;6")
        Color.Grey -> into.append(role).append(";5;7")
        Color.DarkGrey -> into.append(role).append(";5;8")
        Color.Red -> into.append(role).append(";5;9")
        Color.Green -> into.append(role).append(";5;10")
        Color.Yellow -> into.append(role).append(";5;11")
        Color.Blue -> into.append(role).append(";5;12")
        Color.Magenta -> into.append(role).append(";5;13")
        Color.Cyan -> into.append(role).append(";5;14")
        Color.White -> into.append(role).append(";5;15")
        is Color.Rgb -> into.append(role).append(";2;").append(r).append(';').append(g).append(';').append(b)
        is Color.AnsiValue -> into.append(role).append(";5;").append(value)
    }
}
