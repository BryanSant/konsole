package tools.konsole.rich.bar

import tools.konsole.rich.Console
import tools.konsole.rich.Measurable
import tools.konsole.rich.Measurement
import tools.konsole.rich.RenderOptions
import tools.konsole.rich.Segment
import tools.konsole.rich.Style
import tools.konsole.core.style.Color

/**
 * A standalone progress-style bar showing the partial range `[begin, end]` over `[0, size]`.
 * Mirrors `rich.bar.Bar`.
 *
 * Useful as a non-task progress visual — for example, showing buffered bytes within a file.
 */
public class Bar(
    public val size: Double,
    public val begin: Double,
    public val end: Double,
    public val width: Int? = null,
    public val color: Color = Color.Magenta,
    public val backgroundColor: Color = Color.DarkGrey,
    public val completeChar: Char = '━',
    public val backChar: Char = '─',
) : Measurable {

    init {
        require(size >= 0.0) { "size must be >= 0, got $size" }
        require(begin in 0.0..size) { "begin out of [0, size]: $begin" }
        require(end in begin..size) { "end out of [begin, size]: $end" }
    }

    override fun render(console: Console, options: RenderOptions): Sequence<Segment> = sequence {
        val w = (width ?: options.maxWidth).coerceAtMost(options.maxWidth).coerceAtLeast(1)
        val total = if (size <= 0.0) 1.0 else size
        val beginCol = ((begin / total) * w).toInt().coerceIn(0, w)
        val endCol = ((end / total) * w).toInt().coerceIn(beginCol, w)
        val left = beginCol
        val mid = endCol - beginCol
        val right = w - endCol
        if (left > 0) yield(Segment(backChar.toString().repeat(left), Style(color = backgroundColor)))
        if (mid > 0) yield(Segment(completeChar.toString().repeat(mid), Style(color = color)))
        if (right > 0) yield(Segment(backChar.toString().repeat(right), Style(color = backgroundColor)))
    }

    override fun measure(console: Console, options: RenderOptions): Measurement {
        val w = (width ?: options.maxWidth).coerceAtMost(options.maxWidth)
        return Measurement(w, w)
    }
}
