package tools.konsole.rich.box

/**
 * A box-drawing style — 8 lines of 4 characters each, defining the corners, edges, and
 * row dividers used by [tools.konsole.rich.panel.Panel] and
 * [tools.konsole.rich.table.Table].
 *
 * The 8 lines, in order:
 * ```
 * top         ┌─┬┐    corners + horizontal of the top border
 * head_row    │ ││    interior of header row
 * head        ├─┼┤    divider below header
 * mid         │ ││    interior of body row (also "row")
 * row         ├─┼┤    divider between body rows
 * foot_row    │ ││    interior of footer row
 * foot        ├─┼┤    divider above footer
 * bottom      └─┴┘    bottom border
 * ```
 * Each line's 4 characters are: left, mid, divider, right.
 *
 * This mirrors the structure of `rich.box.Box` exactly so all 18 standard styles port over.
 */
public class Box(
    public val name: String,
    box: String,
    public val asciiSafe: Boolean = false,
) {
    init {
        require(box.lines().filter { it.isNotEmpty() }.size == 8) {
            "Box '$name' must have exactly 8 non-empty lines, got: ${'$'}{box.lines().size}"
        }
    }

    private val lines: List<String> = box.lines().filter { it.isNotEmpty() }.map {
        require(it.length == 4) { "Box '$name' line must be 4 chars, got '${'$'}it' (${'$'}{it.length})" }
        it
    }

    public val topLeft: Char get() = lines[0][0]
    public val topHorizontal: Char get() = lines[0][1]
    public val topDivider: Char get() = lines[0][2]
    public val topRight: Char get() = lines[0][3]

    public val headLeft: Char get() = lines[1][0]
    public val headMid: Char get() = lines[1][1]
    public val headDivider: Char get() = lines[1][2]
    public val headRight: Char get() = lines[1][3]

    public val headRowLeft: Char get() = lines[2][0]
    public val headRowHorizontal: Char get() = lines[2][1]
    public val headRowCross: Char get() = lines[2][2]
    public val headRowRight: Char get() = lines[2][3]

    public val midLeft: Char get() = lines[3][0]
    public val midMid: Char get() = lines[3][1]
    public val midDivider: Char get() = lines[3][2]
    public val midRight: Char get() = lines[3][3]

    public val rowLeft: Char get() = lines[4][0]
    public val rowHorizontal: Char get() = lines[4][1]
    public val rowCross: Char get() = lines[4][2]
    public val rowRight: Char get() = lines[4][3]

    public val footRowLeft: Char get() = lines[5][0]
    public val footRowMid: Char get() = lines[5][1]
    public val footRowDivider: Char get() = lines[5][2]
    public val footRowRight: Char get() = lines[5][3]

    public val footLeft: Char get() = lines[6][0]
    public val footHorizontal: Char get() = lines[6][1]
    public val footCross: Char get() = lines[6][2]
    public val footRight: Char get() = lines[6][3]

    public val bottomLeft: Char get() = lines[7][0]
    public val bottomHorizontal: Char get() = lines[7][1]
    public val bottomDivider: Char get() = lines[7][2]
    public val bottomRight: Char get() = lines[7][3]

    /** Construct the top border for column widths. */
    public fun getTop(widths: List<Int>): String =
        buildBorder(widths, topLeft, topHorizontal, topDivider, topRight)

    /** Construct the divider below the header row. */
    public fun getHeadRow(widths: List<Int>): String =
        buildBorder(widths, headRowLeft, headRowHorizontal, headRowCross, headRowRight)

    /** Construct an interior row separator. */
    public fun getRow(widths: List<Int>): String =
        buildBorder(widths, rowLeft, rowHorizontal, rowCross, rowRight)

    /** Construct the divider above the footer row. */
    public fun getFootRow(widths: List<Int>): String =
        buildBorder(widths, footLeft, footHorizontal, footCross, footRight)

    /** Construct the bottom border. */
    public fun getBottom(widths: List<Int>): String =
        buildBorder(widths, bottomLeft, bottomHorizontal, bottomDivider, bottomRight)

    private fun buildBorder(widths: List<Int>, left: Char, mid: Char, div: Char, right: Char): String =
        buildString {
            append(left)
            for ((i, w) in widths.withIndex()) {
                if (i > 0) append(div)
                repeat(w) { append(mid) }
            }
            append(right)
        }

    override fun toString(): String = "Box($name)"

    public companion object {

        public val ASCII: Box = Box(
            "ASCII",
            "+--+\n| ||\n|-+|\n| ||\n|-+|\n| ||\n|-+|\n+--+\n",
            asciiSafe = true,
        )

        public val ASCII2: Box = Box(
            "ASCII2",
            "+-++\n| ||\n+-++\n| ||\n+-++\n| ||\n+-++\n+-++\n",
            asciiSafe = true,
        )

        public val ASCII_DOUBLE_HEAD: Box = Box(
            "ASCII_DOUBLE_HEAD",
            "+-++\n| ||\n+=++\n| ||\n+-++\n| ||\n+-++\n+-++\n",
            asciiSafe = true,
        )

        public val SQUARE: Box = Box(
            "SQUARE",
            "┌─┬┐\n│ ││\n├─┼┤\n│ ││\n├─┼┤\n│ ││\n├─┼┤\n└─┴┘\n",
        )

        public val SQUARE_DOUBLE_HEAD: Box = Box(
            "SQUARE_DOUBLE_HEAD",
            "┌─┬┐\n│ ││\n╞═╪╡\n│ ││\n├─┼┤\n│ ││\n├─┼┤\n└─┴┘\n",
        )

        public val MINIMAL: Box = Box(
            "MINIMAL",
            "  ╷ \n  │ \n╶─┼╴\n  │ \n╶─┼╴\n  │ \n╶─┼╴\n  ╵ \n",
        )

        public val MINIMAL_HEAVY_HEAD: Box = Box(
            "MINIMAL_HEAVY_HEAD",
            "  ╷ \n  │ \n╺━┿╸\n  │ \n╶─┼╴\n  │ \n╶─┼╴\n  ╵ \n",
        )

        public val MINIMAL_DOUBLE_HEAD: Box = Box(
            "MINIMAL_DOUBLE_HEAD",
            "  ╷ \n  │ \n ═╪ \n  │ \n ─┼ \n  │ \n ─┼ \n  ╵ \n",
        )

        public val SIMPLE: Box = Box(
            "SIMPLE",
            "    \n    \n ── \n    \n    \n    \n    \n    \n",
        )

        public val SIMPLE_HEAD: Box = Box(
            "SIMPLE_HEAD",
            "    \n    \n ── \n    \n    \n    \n    \n    \n",
        )

        public val SIMPLE_HEAVY: Box = Box(
            "SIMPLE_HEAVY",
            "    \n    \n ━━ \n    \n    \n    \n    \n    \n",
        )

        public val HORIZONTALS: Box = Box(
            "HORIZONTALS",
            " ── \n    \n ── \n    \n ── \n    \n ── \n ── \n",
        )

        public val ROUNDED: Box = Box(
            "ROUNDED",
            "╭─┬╮\n│ ││\n├─┼┤\n│ ││\n├─┼┤\n│ ││\n├─┼┤\n╰─┴╯\n",
        )

        public val HEAVY: Box = Box(
            "HEAVY",
            "┏━┳┓\n┃ ┃┃\n┣━╋┫\n┃ ┃┃\n┣━╋┫\n┃ ┃┃\n┣━╋┫\n┗━┻┛\n",
        )

        public val HEAVY_EDGE: Box = Box(
            "HEAVY_EDGE",
            "┏━┯┓\n┃ │┃\n┠─┼┨\n┃ │┃\n┠─┼┨\n┃ │┃\n┠─┼┨\n┗━┷┛\n",
        )

        public val HEAVY_HEAD: Box = Box(
            "HEAVY_HEAD",
            "┌─┬┐\n│ ││\n┝━┿┥\n│ ││\n├─┼┤\n│ ││\n├─┼┤\n└─┴┘\n",
        )

        public val DOUBLE: Box = Box(
            "DOUBLE",
            "╔═╦╗\n║ ║║\n╠═╬╣\n║ ║║\n╠═╬╣\n║ ║║\n╠═╬╣\n╚═╩╝\n",
        )

        public val DOUBLE_EDGE: Box = Box(
            "DOUBLE_EDGE",
            "╔═╤╗\n║ │║\n╟─┼╢\n║ │║\n╟─┼╢\n║ │║\n╟─┼╢\n╚═╧╝\n",
        )

        public val MARKDOWN: Box = Box(
            "MARKDOWN",
            "    \n| ||\n|-||\n| ||\n|-||\n| ||\n|-||\n    \n",
            asciiSafe = true,
        )

        /** All 18 named box styles, indexed by [Box.name]. */
        public val ALL: List<Box> = listOf(
            ASCII, ASCII2, ASCII_DOUBLE_HEAD,
            SQUARE, SQUARE_DOUBLE_HEAD,
            MINIMAL, MINIMAL_HEAVY_HEAD, MINIMAL_DOUBLE_HEAD,
            SIMPLE, SIMPLE_HEAD, SIMPLE_HEAVY,
            HORIZONTALS,
            ROUNDED,
            HEAVY, HEAVY_EDGE, HEAVY_HEAD,
            DOUBLE, DOUBLE_EDGE,
            MARKDOWN,
        )
    }
}
