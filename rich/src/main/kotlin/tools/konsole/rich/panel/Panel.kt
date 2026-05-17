package tools.konsole.rich.panel

import tools.konsole.rich.Console
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Renderable
import tools.konsole.rich.Segment
import tools.konsole.rich.Style
import tools.konsole.rich.box.Box
import tools.konsole.rich.layout.Padding
import tools.konsole.rich.layout.collectLines
import tools.konsole.rich.layout.wrapLines
import tools.konsole.rich.text.Justify
import tools.konsole.rich.Text
import tools.konsole.rich.markup.Markup

/**
 * Wraps a [Renderable] in a bordered box with optional title/subtitle.
 * Mirrors `rich.panel.Panel`.
 */
public class Panel(
    public val renderable: Renderable,
    public val box: Box = Box.ROUNDED,
    public val title: Renderable? = null,
    public val titleAlign: Justify = Justify.Center,
    public val subtitle: Renderable? = null,
    public val subtitleAlign: Justify = Justify.Center,
    public val padding: Padding = Padding(0, 1, 0, 1),
    public val expand: Boolean = true,
    public val style: Style = Style.NULL,
    public val borderStyle: Style = Style.NULL,
    public val width: Int? = null,
) : Measurable {

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> = buildList {
        val outer = (width ?: options.maxWidth).coerceAtMost(options.maxWidth).coerceAtLeast(2)
        val inner = (outer - 2 - padding.horizontal).coerceAtLeast(1)
        val borderS = if (borderStyle.isNull) null else borderStyle
        val bodyS = if (style.isNull) null else style

        addTopBorder(outer, inner, console, options, borderS)
        add(Segment.LINE)

        repeat(padding.top) {
            addRow(outer, inner, listOf(), 0, borderS, bodyS)
            add(Segment.LINE)
        }

        val bodyOpts = options.withMaxWidth(inner)
        val bodyLines = wrapLines(collectLines(renderable.render(console, bodyOpts)), inner)
        for ((i, line) in bodyLines.withIndex()) {
            if (i > 0) add(Segment.LINE)
            addRow(outer, inner, line.segments, line.cells, borderS, bodyS)
        }
        if (bodyLines.isEmpty()) {
            addRow(outer, inner, listOf(), 0, borderS, bodyS)
        }

        repeat(padding.bottom) {
            add(Segment.LINE)
            addRow(outer, inner, listOf(), 0, borderS, bodyS)
        }

        add(Segment.LINE)
        addBottomBorder(outer, inner, console, options, borderS)
    }.asSequence()

    private fun MutableList<Segment>.addRow(
        outer: Int,
        inner: Int,
        bodySegments: List<Segment>,
        bodyCells: Int,
        borderS: Style?,
        bodyS: Style?,
    ) {
        add(Segment(box.midLeft.toString(), borderS))
        if (padding.left > 0) add(if (bodyS != null) Segment(" ".repeat(padding.left), bodyS) else Segment(" ".repeat(padding.left)))
        for (s in bodySegments) add(s)
        val rightFill = (inner - bodyCells).coerceAtLeast(0)
        if (rightFill > 0) add(if (bodyS != null) Segment(" ".repeat(rightFill), bodyS) else Segment(" ".repeat(rightFill)))
        if (padding.right > 0) add(if (bodyS != null) Segment(" ".repeat(padding.right), bodyS) else Segment(" ".repeat(padding.right)))
        add(Segment(box.midRight.toString(), borderS))
    }

    private fun MutableList<Segment>.addTopBorder(
        outer: Int,
        inner: Int,
        console: Console,
        options: RenderOptions,
        borderS: Style?,
    ) {
        val titleText = title?.let { textOf(it, console, options.withMaxWidth(outer - 4)) }
        val titleCells = titleText?.cells ?: 0
        if (titleText == null || titleCells == 0) {
            add(Segment("${box.topLeft}${box.topHorizontal.toString().repeat(outer - 2)}${box.topRight}", borderS))
            return
        }
        val padded = " ".plus(titleText.text).plus(" ")
        val displayLen = titleCells + 2
        val edgeWidth = outer - 2
        val sideTotal = (edgeWidth - displayLen).coerceAtLeast(0)
        val (left, right) = sidesFor(titleAlign, sideTotal)
        add(Segment(box.topLeft.toString(), borderS))
        if (left > 0) add(Segment(box.topHorizontal.toString().repeat(left), borderS))
        add(Segment(padded, null))
        if (right > 0) add(Segment(box.topHorizontal.toString().repeat(right), borderS))
        add(Segment(box.topRight.toString(), borderS))
    }

    private fun MutableList<Segment>.addBottomBorder(
        outer: Int,
        inner: Int,
        console: Console,
        options: RenderOptions,
        borderS: Style?,
    ) {
        val subtitleText = subtitle?.let { textOf(it, console, options.withMaxWidth(outer - 4)) }
        val cells = subtitleText?.cells ?: 0
        if (subtitleText == null || cells == 0) {
            add(Segment("${box.bottomLeft}${box.bottomHorizontal.toString().repeat(outer - 2)}${box.bottomRight}", borderS))
            return
        }
        val padded = " ".plus(subtitleText.text).plus(" ")
        val displayLen = cells + 2
        val edgeWidth = outer - 2
        val sideTotal = (edgeWidth - displayLen).coerceAtLeast(0)
        val (left, right) = sidesFor(subtitleAlign, sideTotal)
        add(Segment(box.bottomLeft.toString(), borderS))
        if (left > 0) add(Segment(box.bottomHorizontal.toString().repeat(left), borderS))
        add(Segment(padded, null))
        if (right > 0) add(Segment(box.bottomHorizontal.toString().repeat(right), borderS))
        add(Segment(box.bottomRight.toString(), borderS))
    }

    private data class FlatText(val text: String, val cells: Int)

    private fun textOf(r: Renderable, console: Console, options: RenderOptions): FlatText {
        val sb = StringBuilder()
        var cells = 0
        for (s in r.render(console, options)) {
            if (s.control is tools.konsole.rich.Control.NewLine) break
            if (s.control != null) continue
            sb.append(s.text)
            cells += s.cellLength
        }
        return FlatText(sb.toString(), cells)
    }

    private fun sidesFor(align: Justify, total: Int): Pair<Int, Int> = when (align) {
        Justify.Left -> 1 to (total - 1).coerceAtLeast(0)
        Justify.Right -> (total - 1).coerceAtLeast(0) to 1
        Justify.Center, Justify.Default, Justify.Full -> {
            val l = total / 2
            l to (total - l)
        }
    }

    override fun measure(console: Console, options: RenderOptions): Measurement {
        val outer = width ?: options.maxWidth
        return Measurement(outer.coerceAtMost(options.maxWidth), outer.coerceAtMost(options.maxWidth))
    }

    public companion object {
        /** Convenience: panel with markup-parsed title/subtitle strings. */
        public fun fit(
            content: Renderable,
            title: String? = null,
            subtitle: String? = null,
            box: Box = Box.ROUNDED,
            borderStyle: Style = Style.NULL,
        ): Panel = Panel(
            renderable = content,
            box = box,
            title = title?.let { Markup.parse(it) },
            subtitle = subtitle?.let { Markup.parse(it) },
            borderStyle = borderStyle,
            expand = false,
        )

        /** Convenience: panel with a string title and string body. */
        public fun of(
            body: String,
            title: String? = null,
            box: Box = Box.ROUNDED,
        ): Panel = Panel(
            renderable = Markup.parse(body),
            box = box,
            title = title?.let { Markup.parse(it) },
        )
    }
}
