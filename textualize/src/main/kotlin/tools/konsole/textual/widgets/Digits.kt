package tools.konsole.textual.widgets

import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Style
import tools.konsole.textual.widget.Widget

/**
 * Large 7-segment-style numeric display. Mirrors Python textual's `Digits`
 * widget — used by Clock, Calculator, Timer apps.
 *
 * Renders the [value] string using 5-row Unicode block glyphs. Supports
 * `0-9`, `:`, `.`, `-`, ` `, and falls back to a 1-cell-wide blank for
 * anything else.
 *
 * @param value the digits/symbols to render.
 */
public open class Digits(
    initialValue: String = "0",
    id: String? = null,
    classes: Set<String> = emptySet(),
) : Widget(id, classes) {

    public var value: String = initialValue
        private set

    /** Replace the rendered value. */
    public fun update(value: String) {
        this.value = value
        refresh()
    }

    override fun render(): Renderable = DigitsRenderable(value, style = Style.NULL)
}

private class DigitsRenderable(val text: String, val style: Style) : tools.konsole.rich.Renderable, tools.konsole.rich.Measurable {
    override fun render(console: tools.konsole.rich.Console, options: tools.konsole.rich.RenderOptions): Sequence<Segment> = sequence {
        val rows = Array(GLYPH_HEIGHT) { StringBuilder() }
        for (ch in text) {
            val glyph = GLYPHS[ch] ?: GLYPHS[' ']!!
            for (i in 0 until GLYPH_HEIGHT) {
                if (rows[i].isNotEmpty()) rows[i].append(' ')
                rows[i].append(glyph[i])
            }
        }
        for ((i, row) in rows.withIndex()) {
            yield(Segment(row.toString(), style))
            if (i < rows.lastIndex) yield(Segment("\n"))
        }
    }

    override fun measure(console: tools.konsole.rich.Console, options: tools.konsole.rich.RenderOptions): tools.konsole.rich.Measurement {
        if (text.isEmpty()) return tools.konsole.rich.Measurement(0, 0)
        val width = text.sumOf { (GLYPHS[it]?.maxOf { row -> row.length } ?: 1) } + (text.length - 1)
        return tools.konsole.rich.Measurement(width, width)
    }
}

private const val GLYPH_HEIGHT = 5

/**
 * 5-row block-glyph table for digits and a handful of separators.
 * Each entry is exactly 5 strings, equal width per glyph.
 * The wide-block style mirrors textual's defaults.
 */
private val GLYPHS: Map<Char, Array<String>> = mapOf(
    '0' to arrayOf(
        "█████",
        "█   █",
        "█   █",
        "█   █",
        "█████",
    ),
    '1' to arrayOf(
        "    █",
        "    █",
        "    █",
        "    █",
        "    █",
    ),
    '2' to arrayOf(
        "█████",
        "    █",
        "█████",
        "█    ",
        "█████",
    ),
    '3' to arrayOf(
        "█████",
        "    █",
        " ████",
        "    █",
        "█████",
    ),
    '4' to arrayOf(
        "█   █",
        "█   █",
        "█████",
        "    █",
        "    █",
    ),
    '5' to arrayOf(
        "█████",
        "█    ",
        "█████",
        "    █",
        "█████",
    ),
    '6' to arrayOf(
        "█████",
        "█    ",
        "█████",
        "█   █",
        "█████",
    ),
    '7' to arrayOf(
        "█████",
        "    █",
        "    █",
        "    █",
        "    █",
    ),
    '8' to arrayOf(
        "█████",
        "█   █",
        "█████",
        "█   █",
        "█████",
    ),
    '9' to arrayOf(
        "█████",
        "█   █",
        "█████",
        "    █",
        "█████",
    ),
    ':' to arrayOf(
        "   ",
        " █ ",
        "   ",
        " █ ",
        "   ",
    ),
    '.' to arrayOf(
        "   ",
        "   ",
        "   ",
        "   ",
        " █ ",
    ),
    '-' to arrayOf(
        "     ",
        "     ",
        "█████",
        "     ",
        "     ",
    ),
    ' ' to arrayOf(
        "   ",
        "   ",
        "   ",
        "   ",
        "   ",
    ),
)
