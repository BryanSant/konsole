package tools.konsole.rich.export

/**
 * A color theme used when exporting console content to HTML or SVG.
 * Mirrors `rich.terminal_theme.TerminalTheme`.
 *
 * @param background background color (rgb triple).
 * @param foreground default text color (rgb triple).
 * @param normal 8 normal-intensity ANSI colors (indices 0-7).
 * @param bright 8 bright ANSI colors (indices 8-15). If `null`, [normal] is reused.
 */
public class TerminalTheme(
    public val background: Triple<Int, Int, Int>,
    public val foreground: Triple<Int, Int, Int>,
    public val normal: List<Triple<Int, Int, Int>>,
    public val bright: List<Triple<Int, Int, Int>>? = null,
) {
    init {
        require(normal.size == 8) { "normal must have exactly 8 colors, was ${normal.size}" }
        require(bright == null || bright.size == 8) { "bright (if provided) must have exactly 8 colors, was ${bright?.size}" }
    }

    /** All 16 ANSI colors (normal + bright); when [bright] is null, normal is repeated. */
    public val ansiColors: List<Triple<Int, Int, Int>> = normal + (bright ?: normal)

    public companion object {
        /** Rich's `DEFAULT_TERMINAL_THEME` — white bg / black fg, standard xterm palette. */
        public val DEFAULT: TerminalTheme = TerminalTheme(
            background = Triple(255, 255, 255),
            foreground = Triple(0, 0, 0),
            normal = listOf(
                Triple(0, 0, 0),
                Triple(128, 0, 0),
                Triple(0, 128, 0),
                Triple(128, 128, 0),
                Triple(0, 0, 128),
                Triple(128, 0, 128),
                Triple(0, 128, 128),
                Triple(192, 192, 192),
            ),
            bright = listOf(
                Triple(128, 128, 128),
                Triple(255, 0, 0),
                Triple(0, 255, 0),
                Triple(255, 255, 0),
                Triple(0, 0, 255),
                Triple(255, 0, 255),
                Triple(0, 255, 255),
                Triple(255, 255, 255),
            ),
        )

        /** Rich's `MONOKAI`. */
        public val MONOKAI: TerminalTheme = TerminalTheme(
            background = Triple(12, 12, 12),
            foreground = Triple(217, 217, 217),
            normal = listOf(
                Triple(26, 26, 26),
                Triple(244, 0, 95),
                Triple(152, 224, 36),
                Triple(253, 151, 31),
                Triple(157, 101, 255),
                Triple(244, 0, 95),
                Triple(88, 209, 235),
                Triple(196, 197, 181),
            ),
            bright = listOf(
                Triple(98, 94, 76),
                Triple(244, 0, 95),
                Triple(152, 224, 36),
                Triple(224, 213, 97),
                Triple(157, 101, 255),
                Triple(244, 0, 95),
                Triple(88, 209, 235),
                Triple(246, 246, 239),
            ),
        )

        /** Rich's `DIMMED_MONOKAI`. */
        public val DIMMED_MONOKAI: TerminalTheme = TerminalTheme(
            background = Triple(25, 25, 25),
            foreground = Triple(185, 188, 186),
            normal = listOf(
                Triple(58, 61, 67),
                Triple(190, 63, 72),
                Triple(135, 154, 59),
                Triple(197, 166, 53),
                Triple(79, 118, 161),
                Triple(133, 92, 141),
                Triple(87, 143, 164),
                Triple(185, 188, 186),
            ),
            bright = listOf(
                Triple(136, 137, 135),
                Triple(251, 0, 31),
                Triple(15, 114, 47),
                Triple(196, 112, 51),
                Triple(24, 109, 227),
                Triple(251, 0, 103),
                Triple(46, 112, 109),
                Triple(253, 255, 185),
            ),
        )

        /** Rich's `NIGHT_OWLISH`. */
        public val NIGHT_OWLISH: TerminalTheme = TerminalTheme(
            background = Triple(255, 255, 255),
            foreground = Triple(64, 63, 83),
            normal = listOf(
                Triple(1, 22, 39),
                Triple(211, 66, 62),
                Triple(42, 162, 152),
                Triple(218, 170, 1),
                Triple(72, 118, 214),
                Triple(64, 63, 83),
                Triple(8, 145, 106),
                Triple(122, 129, 129),
            ),
            bright = listOf(
                Triple(122, 129, 129),
                Triple(247, 110, 110),
                Triple(73, 208, 197),
                Triple(218, 194, 107),
                Triple(92, 167, 228),
                Triple(105, 112, 152),
                Triple(0, 201, 144),
                Triple(152, 159, 177),
            ),
        )

        /** Rich's `SVG_EXPORT_THEME` — used as the default for `Console.exportSvg`. */
        public val SVG_EXPORT_THEME: TerminalTheme = TerminalTheme(
            background = Triple(41, 41, 41),
            foreground = Triple(197, 200, 198),
            normal = listOf(
                Triple(75, 78, 85),
                Triple(204, 85, 90),
                Triple(152, 168, 75),
                Triple(208, 179, 68),
                Triple(96, 138, 177),
                Triple(152, 114, 159),
                Triple(104, 160, 179),
                Triple(197, 200, 198),
            ),
            bright = listOf(
                Triple(154, 155, 153),
                Triple(255, 38, 39),
                Triple(0, 130, 61),
                Triple(208, 132, 66),
                Triple(25, 132, 233),
                Triple(255, 44, 122),
                Triple(57, 130, 128),
                Triple(253, 253, 197),
            ),
        )
    }
}
