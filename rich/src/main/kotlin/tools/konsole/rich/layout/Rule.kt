package tools.konsole.rich.layout

import tools.konsole.rich.Console
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Segment
import tools.konsole.rich.Style
import tools.konsole.rich.text.Justify

/**
 * A horizontal divider with an optional title. Mirrors `rich.rule.Rule`.
 */
public class Rule(
    public val title: String? = null,
    public val style: Style = Style.NULL,
    public val char: Char = '─',
    public val align: Justify = Justify.Center,
) : Measurable {

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> = sequence {
        val width = options.maxWidth
        val displayStyle = if (style.isNull) null else style
        if (title.isNullOrBlank()) {
            yield(Segment(char.toString().repeat(width), displayStyle))
            return@sequence
        }
        val padded = " $title "
        val titleLen = padded.length
        if (titleLen >= width) {
            yield(Segment(padded.take(width), displayStyle))
            return@sequence
        }
        val sideTotal = width - titleLen
        val (leftCount, rightCount) = when (align) {
            Justify.Left -> 1 to (sideTotal - 1)
            Justify.Right -> (sideTotal - 1) to 1
            Justify.Center, Justify.Default, Justify.Full -> {
                val l = sideTotal / 2
                l to (sideTotal - l)
            }
        }
        if (leftCount > 0) yield(Segment(char.toString().repeat(leftCount), displayStyle))
        yield(Segment(padded, displayStyle))
        if (rightCount > 0) yield(Segment(char.toString().repeat(rightCount), displayStyle))
    }

    override fun measure(console: Console, options: RenderOptions): Measurement =
        Measurement(options.maxWidth, options.maxWidth)
}
